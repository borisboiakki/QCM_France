#!/usr/bin/env python3
"""
Contrôle de cohérence des fichiers de questions (les 3 QCM + les mises en situation).

À exécuter après chaque lot de rédaction, avant de committer :

    python3 scripts/check_questions_consistency.py

Vérifications :
  - structure : champs obligatoires, `correctAnswer` ∈ A-D, 4 propositions non vides et distinctes,
    thème parmi les 5 thèmes officiels ;
  - ids uniques sur l'ensemble des fichiers, et dans la plage réservée à leur fichier ;
  - variantes : mêmes contrôles que le jeu de base, et bonne réponse différente de celle du jeu
    de base (une variante qui redonne la même bonne réponse n'apporte rien) ;
  - cohérence inter-QCM : un même libellé de question présent dans deux fichiers doit y avoir la
    même bonne réponse (le thème, lui, peut légitimement différer : chaque fichier suit le
    classement de sa liste officielle) ;
  - couverture : effectifs par thème comparés aux listes officielles, et minimum requis par le
    tirage d'examen (13 questions dans « Histoire, géographie et culture », 6 en « Système
    institutionnel et politique », 3 dans les trois autres) ;
  - indice de longueur : une bonne réponse nettement plus longue que tous ses distracteurs se
    repère sans lire l'énoncé (cf. issue #43). Le contrôle porte sur les fichiers dont les
    détrompeurs ont été repris à ce titre (`LENGTH_CUE_FILES`, jeu de base et variantes) ; les
    listes non encore reprises en sont exclues, faute de quoi l'avertissement noierait les
    autres.

Sortie : 0 si tout est conforme, 1 s'il y a au moins une erreur. Les écarts de couverture et
l'indice de longueur sont signalés en avertissement et ne font pas échouer le script.
"""

import json
import os
import sys
import unicodedata
from collections import Counter, defaultdict

RAW = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")

THEMES = [
    "Principes et valeurs de la République",
    "Système institutionnel et politique",
    "Droits et devoirs",
    "Histoire, géographie et culture",
    "Vivre dans la société française",
]

# fichier -> (libellé, plage d'ids réservée)
FILES = {
    "questions.json": ("Naturalisation", (1, 999)),
    "situational_questions.json": ("Mises en situation (communes)", (1000, 1999)),
    "questions_cr.json": ("Carte de résident", (2001, 2999)),
    "questions_csp.json": ("Carte de séjour pluriannuelle", (3001, 3999)),
}

# Effectifs des listes officielles publiées par le ministère de l'Intérieur.
OFFICIAL_COUNTS = {
    "questions.json": dict(zip(THEMES, [39, 55, 37, 83, 44])),
    "questions_cr.json": dict(zip(THEMES, [40, 50, 38, 49, 32])),
    "questions_csp.json": dict(zip(THEMES, [37, 46, 30, 47, 31])),
}

# Minimum requis par thème pour qu'un examen complet de 40 questions soit tirable
# (cf. connaissanceCounts dans QuestionRepository).
DRAW_MINIMUM = dict(zip(THEMES, [3, 6, 3, 13, 3]))

LETTERS = ("A", "B", "C", "D")
OPTION_FIELDS = [f"option{letter}" for letter in LETTERS]

# Indice de longueur (cf. issue #43) : une bonne réponse est signalée quand elle dépasse son plus
# long distracteur à la fois d'un nombre absolu de caractères et d'un facteur — les deux conditions
# ensemble, pour ne pas alerter sur un simple écart de formulation entre propositions déjà
# comparables. Chaque liste rejoint ce contrôle une fois ses détrompeurs repris ; les deux autres
# QCM suivront lot par lot.
LENGTH_CUE_FILES = {"situational_questions.json", "questions.json"}
LENGTH_CUE_ABSOLUTE = 15
LENGTH_CUE_RATIO = 1.4


def norm(text):
    """Normalise un libellé pour comparer deux questions à la ponctuation près."""
    text = unicodedata.normalize("NFKD", text.lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    return "".join(c for c in text if c.isalnum())


def check_answer_set(where, obj, errors, require_text=True):
    """Contrôle un jeu de réponses (question de base ou variante)."""
    for field in OPTION_FIELDS:
        if not obj.get(field, "").strip():
            errors.append(f"{where} : proposition « {field} » vide ou absente")
    options = [obj.get(f, "").strip() for f in OPTION_FIELDS]
    duplicates = [opt for opt, n in Counter(options).items() if opt and n > 1]
    for opt in duplicates:
        errors.append(f"{where} : proposition en double « {opt} »")
    if obj.get("correctAnswer") not in LETTERS:
        errors.append(f"{where} : correctAnswer = {obj.get('correctAnswer')!r} (attendu A, B, C ou D)")
    if require_text and not obj.get("text", "").strip():
        errors.append(f"{where} : énoncé vide")


def check_length_cue(where, obj, warnings):
    """Signale un jeu de réponses dont la bonne réponse est repérable à sa seule longueur."""
    if obj.get("correctAnswer") not in LETTERS:
        return
    answer_length = len(obj.get(f"option{obj['correctAnswer']}", ""))
    longest_decoy = max(
        len(obj.get(f"option{letter}", ""))
        for letter in LETTERS
        if letter != obj["correctAnswer"]
    )
    if (answer_length - longest_decoy > LENGTH_CUE_ABSOLUTE
            and answer_length > LENGTH_CUE_RATIO * longest_decoy):
        warnings.append(
            f"{where} : bonne réponse de {answer_length} caractères contre {longest_decoy} "
            f"pour le plus long distracteur — repérable sans lire l'énoncé, allonger un détrompeur"
        )


def main():
    errors, warnings = [], []
    seen_ids = {}
    by_text = defaultdict(list)

    for filename, (label, (id_min, id_max)) in FILES.items():
        path = os.path.join(RAW, filename)
        if not os.path.exists(path):
            errors.append(f"{filename} : fichier introuvable")
            continue
        with open(path, encoding="utf-8") as f:
            questions = json.load(f)

        for q in questions:
            qid = q.get("id")
            where = f"{filename} #{qid}"

            if qid in seen_ids:
                errors.append(f"{where} : id déjà utilisé dans {seen_ids[qid]}")
            else:
                seen_ids[qid] = filename
            if not isinstance(qid, int) or not (id_min <= qid <= id_max):
                errors.append(f"{where} : id hors de la plage réservée {id_min}-{id_max}")

            if q.get("theme") not in THEMES:
                errors.append(f"{where} : thème inconnu « {q.get('theme')} »")

            check_answer_set(where, q, errors)

            checks_length = filename in LENGTH_CUE_FILES
            if checks_length:
                check_length_cue(where, q, warnings)

            base_answer = q.get(f"option{q['correctAnswer']}") if q.get("correctAnswer") in LETTERS else None
            for i, variant in enumerate(q.get("variants", []), start=1):
                vwhere = f"{where} variante {i}"
                check_answer_set(vwhere, variant, errors, require_text=False)
                if variant.get("correctAnswer") in LETTERS:
                    variant_answer = variant.get(f"option{variant['correctAnswer']}")
                    if variant_answer == base_answer:
                        errors.append(f"{vwhere} : même bonne réponse que le jeu de base")
                if checks_length:
                    check_length_cue(vwhere, variant, warnings)

            if q.get("text"):
                by_text[norm(q["text"])].append((filename, qid, q))

        # Couverture par thème face à la liste officielle
        expected = OFFICIAL_COUNTS.get(filename)
        if expected:
            actual = Counter(q["theme"] for q in questions)
            for theme in THEMES:
                got, want = actual.get(theme, 0), expected[theme]
                if got != want:
                    warnings.append(
                        f"{label} — « {theme} » : {got} question(s) sur {want} de la liste officielle"
                    )
                if got < DRAW_MINIMUM[theme]:
                    warnings.append(
                        f"{label} — « {theme} » : {got} question(s), il en faut {DRAW_MINIMUM[theme]} "
                        f"pour qu'un examen complet de 40 questions soit tirable"
                    )

    # Cohérence inter-QCM : même libellé ⇒ même bonne réponse
    for entries in by_text.values():
        if len(entries) < 2:
            continue
        answers = {norm(q.get(f"option{q['correctAnswer']}", "")) for _, _, q in entries
                   if q.get("correctAnswer") in LETTERS}
        if len(answers) > 1:
            where = ", ".join(f"{f} #{i}" for f, i, _ in entries)
            errors.append(
                f"Bonnes réponses divergentes pour un libellé identique ({where}) : "
                f"« {entries[0][2]['text'][:70]}… »"
            )

    for warning in warnings:
        print(f"⚠  {warning}")
    for error in errors:
        print(f"✗  {error}")

    total = len(seen_ids)
    if errors:
        print(f"\n{len(errors)} erreur(s) sur {total} questions.")
        return 1
    print(f"\n✓ {total} questions contrôlées, aucune erreur"
          f"{f' ({len(warnings)} avertissement(s))' if warnings else ''}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
