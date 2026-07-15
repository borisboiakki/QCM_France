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


def slug_from_url(url: str) -> str:
    path = urlparse(url).path.strip("/")
    return path.split("/")[-1] if path else ""


def _norm_slug(s: str) -> str:
    """Décode l'URL, retire les accents, ne garde que [a-z0-9-] pour comparer des slugs."""
    s = unquote(s)
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9-]", "", s.lower())


def is_subfiche_url(href_abs: str, theme_slug: str) -> bool:
    """True si `href_abs` est une sous-page (fiche) du thème `theme_slug`."""
    p = urlparse(href_abs)
    if p.netloc and p.netloc != urlparse(BASE).netloc:
        return False
    parts = [seg for seg in p.path.split("/") if seg]
    # attend : fiches-par-thematiques / <slug-thème> / <slug-fiche>[/...]
    if len(parts) < 3 or parts[0] != "fiches-par-thematiques":
        return False
    # le 2e segment doit correspondre au thème (tolère accents encodés dans l'URL)
    return _norm_slug(parts[1]) == _norm_slug(theme_slug)


def discover_fiche_urls(index_html: str, index_url: str, theme_slug: str) -> list[str]:
    soup = BeautifulSoup(index_html, "html.parser")
    urls: list[str] = []
    seen: set[str] = set()
    for a in soup.find_all("a", href=True):
        href_abs = urljoin(index_url, a["href"]).split("#")[0].rstrip("/") + "/"
        if href_abs.rstrip("/") == index_url.rstrip("/"):
            continue  # l'index lui-même
        if not is_subfiche_url(href_abs, theme_slug):
            continue
        if href_abs in seen:
            continue
        seen.add(href_abs)
        urls.append(href_abs)
    return urls


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


def build_fiche(url: str, theme_slug: str, dump_dir: str | None) -> dict | None:
    html = fetch_html(url)
    if dump_dir:
        _write_dump(dump_dir, url, html)
    title, content_html = extract_main_html(html)
    markdown = html_to_markdown(content_html)
    if not markdown:
        sys.stderr.write(f"  ! contenu vide pour {url}\n")
        return None
    fiche_slug = _norm_slug(slug_from_url(url)) or "fiche"
    return {
        # id unique sur tout le dataset : préfixé par le slug du thème. Slug normalisé en
        # ASCII (pas de % ni d'accent) → sûr comme argument de navigation dans l'app.
        "id": f"{theme_slug}__{fiche_slug}",
        "title": title or fiche_slug.replace("-", " ").capitalize(),
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
        sys.stderr.write(f"[{theme_name}] index {index_url}\n")
        try:
            index_html = fetch_html(index_url)
        except Exception as exc:  # noqa: BLE001
            sys.stderr.write(f"  ! échec index ({exc})\n")
            index_html = ""
        if args.dump_html and index_html:
            _write_dump(args.dump_html, index_url, index_html)

        fiche_urls = discover_fiche_urls(index_html, index_url, slug) if index_html else []
        sys.stderr.write(f"  {len(fiche_urls)} fiche(s) découverte(s)\n")

        fiches = []
        # Inclure la page d'index elle-même comme première « fiche » (aperçu du thème).
        if index_html:
            idx_title, idx_content = extract_main_html(index_html)
            idx_md = html_to_markdown(idx_content)
            if idx_md:
                fiches.append({
                    "id": f"{slug}__index",
                    "title": idx_title or theme_name,
                    "url": index_url,
                    "markdown": idx_md,
                })

        for f_url in fiche_urls:
            time.sleep(REQUEST_DELAY_S)
            try:
                fiche = build_fiche(f_url, slug, args.dump_html)
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
