#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fetch_fiches.py — Dump hors-ligne des « fiches par thématiques » officielles.

Récupère l'arborescence complète des fiches du site officiel
`formation-civique.interieur.gouv.fr/fiches-par-thematiques/` (5 thèmes → toutes
leurs sous-fiches) et écrit `app/src/main/res/raw/fiches.json`, consommé par
l'app Android en mode 100 % hors-ligne.

⚠️  À exécuter dans la pipeline GitHub Actions `update-fiches.yml` (les runners
    GitHub ne sont pas soumis à la politique réseau qui bloque le domaine
    gouvernemental depuis les sessions Claude Code). Déclenchement manuel.

Dépendances : requests, beautifulsoup4, markdownify.
    pip install requests beautifulsoup4 markdownify

Usage :
    python scripts/fetch_fiches.py                       # écrit le JSON par défaut
    python scripts/fetch_fiches.py --out chemin.json     # chemin de sortie
    python scripts/fetch_fiches.py --dump-html html/     # archive le HTML brut (debug sélecteurs)

Robustesse : l'extraction du contenu est **générique** (repérage du plus gros
conteneur d'article, cf. extract_main_html) plutôt que basée sur des sélecteurs
CSS précis, car le DOM exact ne peut pas être validé depuis l'environnement de
développement. Si `requests` renvoie une coquille vide (site rendu côté client
en JavaScript), basculer sur Playwright — voir fetch_html().
"""

from __future__ import annotations

import argparse
import datetime as _dt
import json
import os
import re
import sys
import time
import unicodedata
from urllib.parse import unquote, urljoin, urlparse

try:
    import requests
    from bs4 import BeautifulSoup
    from markdownify import markdownify as md
except ImportError as exc:  # pragma: no cover - message d'aide en CI
    sys.stderr.write(
        "Dépendance manquante : {}\n"
        "Installer avec : pip install requests beautifulsoup4 markdownify\n".format(exc)
    )
    raise

BASE = "https://formation-civique.interieur.gouv.fr"
INDEX = BASE + "/fiches-par-thematiques/"

# slug d'URL -> nom officiel exact du thème (aligné sur les 5 thèmes de l'app).
THEMES: list[tuple[str, str]] = [
    ("principes-et-valeurs-de-la-republique", "Principes et valeurs de la République"),
    ("systeme-institutionnel-et-politique", "Système institutionnel et politique"),
    ("droits-et-devoirs", "Droits et devoirs"),
    ("histoire-geographie-et-culture", "Histoire, géographie et culture"),
    ("vivre-dans-la-societe-francaise", "Vivre dans la société française"),
]

# Certains slugs comportent des caractères encodés/accentués dans l'URL réelle.
# On tente d'abord le slug ASCII ; à défaut, la découverte via l'index rattrape.
THEME_URL_OVERRIDES = {
    "vivre-dans-la-societe-francaise":
        BASE + "/fiches-par-thematiques/vivre-dans-la-societe-fran%C3%A7aise/",
}

USER_AGENT = (
    "QCM-France-fiches-bot/1.0 (+https://github.com/borisboiakki/qcm_france) "
    "content sync for offline civic-exam study app"
)
REQUEST_DELAY_S = 1.0          # politesse entre deux requêtes
TIMEOUT_S = 30
MAX_DEPTH = 6                  # garde-fou de profondeur du crawl par thème
MAX_PAGES = 400               # garde-fou du nombre de pages visitées par thème

_session = requests.Session()
_session.headers.update({"User-Agent": USER_AGENT})


def theme_index_url(slug: str) -> str:
    return THEME_URL_OVERRIDES.get(slug, BASE + f"/fiches-par-thematiques/{slug}/")


def fetch_html(url: str) -> str:
    """Récupère le HTML d'une page.

    Fallback JS : si le site s'avère rendu côté client (contenu vide), remplacer
    l'implémentation ci-dessous par Playwright :

        from playwright.sync_api import sync_playwright
        with sync_playwright() as p:
            b = p.chromium.launch()
            pg = b.new_page(user_agent=USER_AGENT)
            pg.goto(url, wait_until="networkidle")
            html = pg.content()
            b.close()
            return html
    """
    resp = _session.get(url, timeout=TIMEOUT_S)
    resp.raise_for_status()
    resp.encoding = resp.apparent_encoding or "utf-8"
    return resp.text


def _norm_slug(s: str) -> str:
    """Décode l'URL, retire les accents, ne garde que [a-z0-9-] pour comparer des slugs."""
    s = unquote(s)
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9-]", "", s.lower())


def _norm_url(base: str, href: str) -> str:
    """URL absolue normalisée : sans fragment ni query, avec un slash final."""
    u = urljoin(base, href).split("#")[0].split("?")[0]
    return u.rstrip("/") + "/"


def _seg_keys(url: str) -> list[str]:
    """Segments de chemin normalisés (décodés, sans accents) pour comparer des URLs."""
    return [_norm_slug(s) for s in urlparse(url).path.split("/") if s]


def direct_child_urls(html: str, page_url: str) -> list[str]:
    """Liens « enfants directs » : exactement un segment plus bas que `page_url`, même sous-arbre.

    Sert à distinguer une page **intermédiaire** (qui liste des sous-pages) d'une **feuille** de
    contenu (sans enfant). La comparaison se fait sur des segments normalisés pour tolérer les
    accents encodés dans les URLs (ex. « société-fran%C3%A7aise »).
    """
    base_keys = _seg_keys(page_url)
    base_host = urlparse(BASE).netloc
    children: list[str] = []
    seen: set[str] = set()
    soup = BeautifulSoup(html, "html.parser")
    for a in soup.find_all("a", href=True):
        cu = _norm_url(page_url, a["href"])
        p = urlparse(cu)
        if p.netloc and p.netloc != base_host:
            continue
        keys = _seg_keys(cu)
        # exactement un cran plus bas ET dans le sous-arbre de page_url
        if len(keys) != len(base_keys) + 1 or keys[:len(base_keys)] != base_keys:
            continue
        if cu in seen:
            continue
        seen.add(cu)
        children.append(cu)
    return children


def crawl_theme_leaves(theme_slug: str, dump_dir: str | None) -> list[tuple[str, str]]:
    """Parcourt récursivement le sous-arbre du thème et renvoie les **feuilles** `(url, html)`.

    Une page ayant des enfants directs est une page intermédiaire (index de thème ou de catégorie,
    listant des liens « Pages ») : on descend sans l'émettre. Une page sans enfant est une vraie
    fiche de contenu : on l'émet. Résultat : uniquement le contenu réel, sans les pages de liens.
    """
    index_url = theme_index_url(theme_slug)
    visited: set[str] = set()
    leaves: list[tuple[str, str]] = []

    def visit(url: str, depth: int) -> None:
        key = "/".join(_seg_keys(url))
        if key in visited or len(visited) >= MAX_PAGES:
            return
        visited.add(key)
        try:
            html = fetch_html(url)
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write(f"  ! échec {url} ({exc})\n")
            return
        if dump_dir:
            _write_dump(dump_dir, url, html)
        time.sleep(REQUEST_DELAY_S)

        children = [] if depth >= MAX_DEPTH else direct_child_urls(html, url)
        children = [c for c in children if "/".join(_seg_keys(c)) not in visited]
        if children:
            for c in children:
                visit(c, depth + 1)
        else:
            leaves.append((url, html))

    visit(index_url, 0)
    return leaves


def extract_main_html(page_html: str) -> tuple[str, str]:
    """Retourne (titre, html_du_contenu_principal).

    Heuristique générique : privilégie <main>/<article>, sinon le plus gros bloc
    de texte, puis nettoie les éléments non-contenu (nav, footer, scripts…).
    """
    soup = BeautifulSoup(page_html, "html.parser")

    # Titre : <h1> sinon <title>
    title = ""
    h1 = soup.find("h1")
    if h1 and h1.get_text(strip=True):
        title = h1.get_text(strip=True)
    elif soup.title and soup.title.get_text(strip=True):
        title = soup.title.get_text(strip=True)

    for tag in soup(["script", "style", "noscript", "nav", "header", "footer", "form", "aside"]):
        tag.decompose()

    container = soup.find("main") or soup.find("article")
    if container is None:
        # plus gros conteneur par longueur de texte
        candidates = soup.find_all(["main", "article", "section", "div"])
        container = max(
            candidates,
            key=lambda t: len(t.get_text(strip=True)),
            default=soup.body or soup,
        )

    return title, str(container)


def html_to_markdown(content_html: str) -> str:
    text = md(content_html, heading_style="ATX", bullets="-")
    # normalise les lignes vides multiples
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def build_fiche(url: str, html: str, theme_slug: str) -> dict | None:
    """Construit l'entrée fiche à partir du HTML déjà téléchargé (feuille de contenu)."""
    title, content_html = extract_main_html(html)
    markdown = html_to_markdown(content_html)
    if not markdown:
        sys.stderr.write(f"  ! contenu vide pour {url}\n")
        return None
    # id unique sur tout le dataset : slug du thème + chemin relatif (sous fiches-par-thematiques/
    # <thème>) normalisé en ASCII → pas de collision entre catégories, sûr comme argument de
    # navigation dans l'app.
    rel = _seg_keys(url)[2:]  # segments sous « fiches-par-thematiques / <thème> »
    fiche_key = "__".join(rel) if rel else "index"
    return {
        "id": f"{theme_slug}__{fiche_key}",
        "title": title or (rel[-1].replace("-", " ").capitalize() if rel else theme_slug),
        "url": url,
        "markdown": markdown,
    }


def _write_dump(dump_dir: str, url: str, html: str) -> None:
    os.makedirs(dump_dir, exist_ok=True)
    name = (urlparse(url).path.strip("/").replace("/", "_") or "index") + ".html"
    with open(os.path.join(dump_dir, name), "w", encoding="utf-8") as fh:
        fh.write(html)


def main() -> int:
    parser = argparse.ArgumentParser(description="Dump des fiches thématiques officielles.")
    default_out = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app", "src", "main", "res", "raw", "fiches.json",
    )
    parser.add_argument("--out", default=default_out, help="chemin du fichier JSON de sortie")
    parser.add_argument("--dump-html", default=None, help="dossier où archiver le HTML brut (debug)")
    args = parser.parse_args()

    themes_out = []
    total_fiches = 0
    for slug, theme_name in THEMES:
        index_url = theme_index_url(slug)
        sys.stderr.write(f"[{theme_name}] crawl {index_url}\n")

        # Descend récursivement jusqu'aux feuilles de contenu (pages sans sous-pages) : les pages
        # intermédiaires (index du thème, pages de catégorie listant des liens) ne sont pas émises.
        leaves = crawl_theme_leaves(slug, args.dump_html)
        sys.stderr.write(f"  {len(leaves)} fiche(s) de contenu trouvée(s)\n")

        fiches = []
        for f_url, f_html in leaves:
            try:
                fiche = build_fiche(f_url, f_html, slug)
            except Exception as exc:  # noqa: BLE001
                sys.stderr.write(f"  ! échec fiche {f_url} ({exc})\n")
                continue
            if fiche:
                fiches.append(fiche)

        total_fiches += len(fiches)
        themes_out.append({
            "theme": theme_name,
            "url": index_url,
            "fiches": fiches,
        })
        time.sleep(REQUEST_DELAY_S)

    data = {
        "version": 1,
        "generatedAt": _dt.date.today().isoformat(),
        "source": INDEX,
        "themes": themes_out,
    }

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    sys.stderr.write(f"\n✓ {total_fiches} fiche(s) sur {len(themes_out)} thème(s) → {args.out}\n")
    if total_fiches == 0:
        sys.stderr.write(
            "ATTENTION : 0 fiche extraite. Le site est probablement rendu côté JS — "
            "activer le fallback Playwright dans fetch_html() (voir docstring).\n"
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
