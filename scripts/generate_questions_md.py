#!/usr/bin/env python3
"""
Génère QUESTIONS.md depuis app/src/main/res/raw/questions.json.
Usage : python3 scripts/generate_questions_md.py
"""

import json
import os
from collections import defaultdict
from datetime import date

QUESTIONS_JSON = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "questions.json"
)
OUTPUT_MD = os.path.join(os.path.dirname(__file__), "..", "QUESTIONS.md")

THEME_ORDER = [
    "Principes et valeurs de la République",
    "Système institutionnel et politique",
    "Droits et devoirs",
    "Histoire, géographie et culture",
    "Vivre dans la société française",
]

LETTER_TO_FIELD = {"A": "optionA", "B": "optionB", "C": "optionC", "D": "optionD"}


def escape_md(text: str) -> str:
    """Échappe les caractères Markdown dans les cellules de tableau."""
    return text.replace("|", "\\|").replace("\n", " ").strip()


def source_cell(source: str) -> str:
    if source:
        return f"[source]({source})"
    return ""


def main():
    with open(QUESTIONS_JSON, encoding="utf-8") as f:
        questions = json.load(f)

    by_theme = defaultdict(list)
    for q in questions:
        by_theme[q["theme"]].append(q)

    lines = [
        "# QCM France — Liste des questions et réponses",
        "",
        f"> Généré automatiquement le {date.today().isoformat()} "
        f"à partir de `app/src/main/res/raw/questions.json`  ",
        f"> **{len(questions)} questions** réparties en {len(by_theme)} thèmes.",
        "",
        "> **Note :** dans l'application, l'ordre des propositions (A, B, C, D) est mélangé "
        "aléatoirement à chaque examen. L'ordre affiché dans ce tableau correspond à l'ordre "
        "original du fichier de données et peut donc différer de celui présenté lors du test.",
        "",
        "---",
        "",
        "## Table des matières",
        "",
    ]

    for theme in THEME_ORDER:
        anchor = theme.lower().replace(" ", "-").replace("'", "").replace(",", "")
        count = len(by_theme.get(theme, []))
        lines.append(f"- [{theme}](#{anchor}) ({count} questions)")

    lines += ["", "---", ""]

    for theme in THEME_ORDER:
        qs = by_theme.get(theme, [])
        if not qs:
            continue

        lines += [
            f"## {theme}",
            "",
            f"*{len(qs)} questions dans la base*",
            "",
            "| # | Question | A | B | C | D | Bonne réponse | Source |",
            "|---|---|---|---|---|---|---|---|",
        ]

        for q in sorted(qs, key=lambda x: x["id"]):
            correct_letter = q["correctAnswer"]
            correct_text = q.get(LETTER_TO_FIELD.get(correct_letter, ""), "")
            row = "| {} | {} | {} | {} | {} | {} | **{}** — {} | {} |".format(
                q["id"],
                escape_md(q["text"]),
                escape_md(q["optionA"]),
                escape_md(q["optionB"]),
                escape_md(q["optionC"]),
                escape_md(q["optionD"]),
                correct_letter,
                escape_md(correct_text),
                source_cell(q.get("source", "")),
            )
            lines.append(row)

        lines += ["", "---", ""]

    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    print(f"✓ {OUTPUT_MD} généré ({len(questions)} questions, {len(by_theme)} thèmes)")


if __name__ == "__main__":
    main()
