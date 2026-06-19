# Concordance PDF officiel ↔ Excel QCM

**Date d'analyse :** 2026-06-19  
**PDF de référence :** `questions_officielles_naturalisation_2025.pdf` (version 2025-12-12)  
**Excel :** `naturalisation_QCM.xlsx`  

---

## Résumé

| Source | Nb questions |
|---|---|
| PDF officiel (questions uniques) | ~258 |
| Excel (avec 4 choix complets) | **201** |
| Questions communes (correspondance exacte ou libellé proche) | ~201 |
| **Questions PDF sans réponses dans l'Excel** | **~57** |

Les **201 questions de l'Excel sont toutes issues du PDF officiel** (libellés identiques ou très proches).  
Le `questions.json` généré est donc valide et exploitable pour l'app Android.

---

## Questions du PDF absentes de l'Excel (sans réponses disponibles)

Ces ~57 questions sont dans la liste officielle mais n'ont pas encore de propositions de réponses dans l'Excel. Elles ne peuvent pas être utilisées dans le QCM tant que les 3 mauvaises réponses ne sont pas ajoutées.

### 15 questions nouvelles (ajout récent au PDF officiel — en gras dans le PDF)

1. Que signifie le mot "fraternité" dans la devise française ?
2. Selon la Constitution, la France est une République...
3. Selon le principe de laïcité, que signifie la neutralité de l'État ?
4. Quel est le dernier État à avoir intégré l'Union Européenne en 2013 ?
5. Les citoyens de l'Union européenne peuvent-ils voter aux élections locales dans un autre État de l'Union ?
6. Quelle est la devise de l'Union européenne ?
7. Qui peut être appelé à faire partie d'un jury d'assises en France ?
8. Quel est l'objectif des lois scolaires de la IIIe République ?
9. Pourquoi l'année 1958 est-elle importante pour la France ?
10. Simone Veil est une figure importante de l'histoire française. Elle a notamment :
11. Qui a rendu l'école gratuite, laïque et obligatoire ?
12. Quelle œuvre a été écrite par Victor Hugo ?
13. Quelle mer se situe entre la France et l'Angleterre ?
14. Quelle chaîne de montagnes est située entre la France et l'Espagne ?
15. Qu'est-ce que le principe de confidentialité dans le domaine de la santé ?

### ~42 questions anciennes non couvertes par l'Excel

(Institutions, laïcité, droits civiques, vie pratique — l'auteur de l'Excel ne les a pas encore traitées)

---

## Fichier généré

`questions.json` — **201 questions**, format :
```json
{
  "id": 1,
  "theme": "Principes et valeurs de la République",
  "text": "...",
  "optionA": "...",
  "optionB": "...",
  "optionC": "...",
  "optionD": "...",
  "correctAnswer": "A",
  "explanation": ""
}
```

Les options A/B/C/D sont mélangées de façon déterministe (seed = id).  
Le champ `explanation` est vide pour l'instant — à enrichir ultérieurement.

---

## Action recommandée

Pour les 57 questions manquantes, deux options :
1. Compléter l'Excel manuellement avec 3 fausses réponses par question
2. Les générer automatiquement via IA (en s'appuyant sur le Livret du citoyen et la Charte)
