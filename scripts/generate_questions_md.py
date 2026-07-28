#!/usr/bin/env python3
"""
Génère QUESTIONS.md depuis les fichiers de questions de app/src/main/res/raw/ :
les listes de connaissances des trois QCM (naturalisation, carte de résident, carte de séjour
pluriannuelle) et les mises en situation, communes aux trois.
Usage : python3 scripts/generate_questions_md.py
"""

import json
import os
from collections import defaultdict
from datetime import date

RAW = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
OUTPUT_MD = os.path.join(os.path.dirname(__file__), "..", "QUESTIONS.md")

SITUATIONAL_JSON = "situational_questions.json"

# (fichier, titre de section, ancre, description)
SECTIONS = [
    (
        "questions.json",
        "Naturalisation",
        "naturalisation",
        "Liste officielle des questions de connaissance de l'examen civique de naturalisation.",
    ),
    (
        "questions_cr.json",
        "Carte de résident (CR)",
        "carte-de-resident-cr",
        "Liste officielle des questions de connaissance de l'examen civique de niveau carte de résident.",
    ),
    (
        "questions_csp.json",
        "Carte de séjour pluriannuelle (CSP)",
        "carte-de-sejour-pluriannuelle-csp",
        "Liste officielle des questions de connaissance de l'examen civique de niveau carte de séjour pluriannuelle.",
    ),
    (
        SITUATIONAL_JSON,
        "Mises en situation (communes aux trois examens)",
        "mises-en-situation-communes-aux-trois-examens",
        "Les questions de mise en situation ne sont pas publiées par le ministère : celles-ci sont "
        "rédigées à partir des fiches thématiques officielles. Elles sont tirées dans les trois examens.",
    ),
]

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
    return f"[source]({source})" if source else ""


def question_row(q: dict, is_situation: bool) -> str:
    correct_letter = q["correctAnswer"]
    correct_text = q.get(LETTER_TO_FIELD.get(correct_letter, ""), "")
    # Questions à plusieurs bonnes réponses valides : le jeu de réponses affiché tourne
    # aléatoirement. On liste ici les bonnes réponses alternatives à titre de référence.
    variants = q.get("variants", [])
    if variants:
        alts = [v.get(LETTER_TO_FIELD.get(v["correctAnswer"], ""), "") for v in variants]
        correct_text += " *(variantes : " + ", ".join(alts) + ")*"
    q_type = "Mise en situation" if is_situation else "Connaissances"
    return "| {} | {} | {} | {} | {} | {} | {} | **{}** — {} | {} |".format(
        q["id"],
        q_type,
        escape_md(q["text"]),
        escape_md(q["optionA"]),
        escape_md(q["optionB"]),
        escape_md(q["optionC"]),
        escape_md(q["optionD"]),
        correct_letter,
        escape_md(correct_text),
        source_cell(q.get("source", "")),
    )


def theme_anchor(section_anchor: str, theme: str) -> str:
    slug = theme.lower().replace(" ", "-").replace("'", "").replace(",", "")
    return f"{section_anchor}-{slug}"


def main():
    sections = []
    for filename, title, anchor, description in SECTIONS:
        with open(os.path.join(RAW, filename), encoding="utf-8") as f:
            questions = json.load(f)
        by_theme = defaultdict(list)
        for q in questions:
            by_theme[q["theme"]].append(q)
        sections.append({
            "filename": filename,
            "title": title,
            "anchor": anchor,
            "description": description,
            "questions": questions,
            "by_theme": by_theme,
            "is_situation": filename == SITUATIONAL_JSON,
        })

    total = sum(len(s["questions"]) for s in sections)

    lines = [
        "# QCM France — Liste des questions et réponses",
        "",
        f"> Généré automatiquement le {date.today().isoformat()} "
        f"à partir des fichiers de `app/src/main/res/raw/`  ",
        f"> **{total} questions** au total, réparties entre les trois examens civiques.",
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

    for section in sections:
        lines.append(
            f"- [{section['title']}](#{section['anchor']}) ({len(section['questions'])} questions)"
        )
        for theme in THEME_ORDER:
            count = len(section["by_theme"].get(theme, []))
            if count:
                lines.append(
                    f"  - [{theme}](#{theme_anchor(section['anchor'], theme)}) ({count})"
                )

    lines += ["", "---", ""]

    for section in sections:
        lines += [
            f"## {section['title']}",
            "",
            section["description"],
            "",
            f"*{len(section['questions'])} questions — `app/src/main/res/raw/{section['filename']}`*",
            "",
        ]

        for theme in THEME_ORDER:
            questions = section["by_theme"].get(theme, [])
            if not questions:
                continue
            lines += [
                f"### {theme}",
                "",
                f"*{len(questions)} questions dans la base*",
                "",
                "| # | Type | Question | A | B | C | D | Bonne réponse | Source |",
                "|---|---|---|---|---|---|---|---|---|",
            ]
            for q in sorted(questions, key=lambda x: x["id"]):
                lines.append(question_row(q, section["is_situation"]))
            lines.append("")

        lines += ["---", ""]

    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    print(f"✓ {OUTPUT_MD} généré ({total} questions, {len(sections)} sections)")


if __name__ == "__main__":
    main()
