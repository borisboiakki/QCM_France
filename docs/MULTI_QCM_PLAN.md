# Multi-QCM — plan et suivi d'avancement

Extension de l'application aux **trois examens civiques officiels** au lieu du seul examen de
naturalisation. Ce document est le point de reprise du développement : il décrit ce qui a été fait,
ce qui reste à faire, et les conventions à respecter pour chaque lot de contenu.

| | |
|---|---|
| Branche | `claude/multi-qcm-expansion-m33wj9` |
| Statut | **Terminé** — les trois listes officielles sont complètes (258 NAT + 209 CR + 191 CSP + 80 mises en situation = 738 questions) |
| Dernière mise à jour | 2026-07-28 |

---

## 1. Objectif

L'utilisateur choisit sur l'écran d'accueil l'examen qu'il prépare ; l'examen blanc,
l'entraînement et les succès s'adaptent au choix.

| Code | Examen | Questions de connaissances (liste officielle) |
|---|---|---|
| `NAT` | Naturalisation | 258 |
| `CR` | Carte de résident | 209 |
| `CSP` | Carte de séjour pluriannuelle | 191 |

**Communs aux trois examens :** les 80 questions de mise en situation (code `ALL`) et les fiches
thématiques officielles hors-ligne.

**Identique pour les trois examens :** 40 questions (28 connaissances + 12 mises en situation),
45 minutes, seuil 32/40, répartition par thème `3/6/3/13/3` + `3/3/3/0/3`, cycle anti-répétition,
pause/reprise. Aucune constante d'examen n'a été modifiée : chaque liste officielle compte au moins
13 questions dans « Histoire, géographie et culture », le tirage reste donc satisfaisable.

### Sources officielles

- Naturalisation : <https://formation-civique.interieur.gouv.fr/examen-civique/>
- CR : <https://formation-civique.interieur.gouv.fr/examen-civique/liste-officielle-des-questions-de-connaissance-cr/>
- CSP : <https://formation-civique.interieur.gouv.fr/examen-civique/liste-officielle-des-questions-de-connaissance-csp/>

Le ministère publie **uniquement les énoncés** : les 4 propositions, la bonne réponse,
l'explication, la source et les variantes sont rédigées pour l'application.

---

## 2. Décisions structurantes

1. **Un fichier JSON par examen.** Chaque fichier reflète fidèlement sa liste officielle, y compris
   les questions dont le libellé est identique dans deux examens : leur jeu de réponses est alors
   **recopié à l'identique** depuis le fichier où il a déjà été rédigé et validé, jamais réécrit.
   `scripts/check_questions_consistency.py` détecte toute divergence de bonne réponse entre
   fichiers. Le **thème** peut légitimement différer d'un examen à l'autre pour un même libellé :
   chaque fichier suit le classement de sa propre liste officielle (2 cas en CR).
2. **`examMode` déduit du fichier de seed**, jamais répété dans le JSON
   (`QuestionRepository.readSeedFile`).
3. **Clés de cycle et de progression préfixées par le code de l'examen**, sauf pour la
   naturalisation qui conserve les clés historiques (`ExamMode.cycleKey` / `ExamMode.trainingKey`) :
   les installations existantes gardent leur progression, leur cycle de tirage et leurs succès.
4. **Succès « Tour complet » par examen** (3 succès), les autres succès d'examen restent globaux.
   Un bloc de 6 succès d'entraînement par examen (5 thèmes + « tous les thèmes »), soit 27 succès.
5. **Écran « S'entraîner » : 3 sections empilées**, une par examen, puis la section « sources
   officielles » (fiches, communes).

---

## 3. Architecture livrée (lot 0)

### Nouveaux fichiers

| Fichier | Rôle |
|---|---|
| `data/model/ExamMode.kt` | Enum des 3 examens + helpers `cycleKey` / `trainingKey` / `fromCode` |
| `res/raw/questions_cr.json` | Liste CR, ids **2001-2999** |
| `res/raw/questions_csp.json` | Liste CSP, ids **3001-3999** |
| `scripts/check_questions_consistency.py` | Contrôle structurel, ids, variantes, cohérence inter-QCM, couverture |
| `docs/MULTI_QCM_PLAN.md` | Ce document |

### Base de données — v11 → v12 (`MIGRATION_11_12`)

```sql
ALTER TABLE questions    ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT';
ALTER TABLE paused_quiz  ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT';
ALTER TABLE quiz_results ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT';
UPDATE questions SET examMode = 'ALL' WHERE isSituation = 1;
```

Les tables `exam_cycle`, `training_progress` et `seen_question` sont **inchangées** : la
distinction par examen passe par la clé (chaîne libre) ou par jointure.

### Plages d'ids réservées

| Plage | Contenu |
|---|---|
| 1 – 999 | Connaissances naturalisation (1-258 utilisés) |
| 1000 – 1999 | Mises en situation, communes (1001-1080 utilisés) |
| 2001 – 2999 | Connaissances carte de résident |
| 3001 – 3999 | Connaissances carte de séjour pluriannuelle |

Les ids CR/CSP suivent **l'ordre de la liste officielle** (thèmes dans l'ordre `Principes`,
`Système`, `Droits`, `Histoire`, `Vivre`) : `2001` est la 1re question de la page CR, `3001` la 1re
de la page CSP. Une question rédigée dans un lot ultérieur reprend donc l'id qui lui est réservé
par sa position dans la liste — les ids ne sont pas séquentiels dans le fichier tant que la
rédaction est en cours, c'est normal.

### Points de vigilance

- **Bumper `QuestionRepository.CONTENT_VERSION`** à chaque modification d'un fichier de questions,
  sinon les applications déjà installées ne voient pas le changement.
- Si un thème comptait moins de questions que le tirage n'en demande, l'examen serait **plus court
  que 40 questions** (`drawIdsFromCycle` ne tire jamais deux fois la même question) et le script de
  contrôle le signalerait en avertissement. Les trois listes étant complètes, le cas ne se produit
  plus — mais le garde-fou reste utile pour un futur QCM en cours de saisie.

---

## 4. Conventions de rédaction des questions

Pour chaque question à rédiger :

- **4 propositions**, une seule défendable. Les 3 distracteurs doivent être **clairement faux** —
  pas une réponse alternative également correcte (piège corrigé en `CONTENT_VERSION` 3 sur la
  naturalisation).
- **Détrompeurs plausibles** (mises en situation, issue #43). Un distracteur caricatural
  — violence, indifférence affichée, illégalité manifeste — s'élimine sans connaître la règle et
  ne teste rien. Chaque mise en situation comporte donc un détrompeur « presque juste », faux pour
  une raison **précise et citable** :
  - le mauvais numéro ou la mauvaise autorité (15 au lieu du 115, préfet au lieu du procureur) ;
  - le mauvais délai ou le mauvais seuil (7 jours au lieu de 14 pour la rétractation) ;
  - le bon réflexe dans le mauvais ordre (saisir le juge avant la réclamation préalable) ;
  - la bonne règle appliquée à la mauvaise personne (neutralité laïque étendue aux usagers) ;
  - une condition inventée mais crédible (« avec l'accord écrit du préfet »).

  Les deux autres distracteurs portent une justification plausible plutôt qu'un refus brut, et
  l'`explanation` dit **pourquoi le piège ne tient pas**.
- **Longueur des propositions comparable.** Une bonne réponse nettement plus longue que ses trois
  distracteurs se repère sans lire l'énoncé : `check_questions_consistency.py` le signale en
  avertissement sur `situational_questions.json`.
- **`explanation`** : 1 à 3 phrases justifiant la bonne réponse, plus une phrase écartant le
  détrompeur plausible pour les mises en situation.
- **`source`** : URL officielle — `legifrance.gouv.fr`, `service-public.fr`,
  `formation-civique.interieur.gouv.fr`, `vie-publique.fr`, `elysee.fr`, `assemblee-nationale.fr`…
- **`variants`** : à ajouter quand plusieurs bonnes réponses sont également valides (« Quel musée
  est situé à Paris ? »). Chaque variante = 4 propositions + sa bonne réponse, **différente** de
  celle du jeu de base. En examen un jeu est tiré au hasard, en entraînement tous sont déroulés.
- **`theme`** : exactement l'un des 5 libellés canoniques (la page CR écrit « Histoire géographie
  et culture » sans virgule → normaliser en « Histoire, géographie et culture »).
- **Énoncé** : reproduire le libellé officiel **à l'identique**.

Après chaque lot :

```bash
python3 scripts/check_questions_consistency.py   # doit sortir sans erreur
python3 scripts/generate_questions_md.py         # régénère QUESTIONS.md
```

puis bumper `CONTENT_VERSION`, cocher le lot ci-dessous, committer et pousser.

---

## 5. Suivi des tâches

### Lot 0 — architecture

- [x] `ExamMode` (codes, libellés, clés de cycle et de progression)
- [x] Colonne `examMode` sur `Question`, `QuizResult`, `PausedQuiz` + `MIGRATION_11_12`
- [x] `QuestionDao` : requêtes filtrées par examen, `countForMode`, `countSeenForMode`
- [x] `QuestionRepository` : seed des 4 fichiers, tirage par examen, `CONTENT_VERSION` = 4
- [x] `TrainingRepository` / `PausedQuizRepository` / `SettingsRepository` (choix persisté)
- [x] Succès : 27 succès, catégories par examen, « Tour complet » par examen
- [x] `HomeScreen` : sélecteur d'examen (la carte des règles est retirée, cf. écran d'aide)
- [x] `TrainingThemesScreen` : 3 sections + section fiches
- [x] Examen / résultat / historique / export : examen passé affiché
- [x] Aide, ressources (liens vers les listes CR et CSP), `README.md`, `CLAUDE.md`
- [x] `check_questions_consistency.py`, `generate_questions_md.py` multi-QCM
- [x] `questions_cr.json` / `questions_csp.json` amorcés avec les questions communes (60 / 44)

### Lots de contenu

Nombres = questions restant à rédiger dans le lot (les questions communes à la naturalisation sont
déjà en place).

| Lot | Examen | Thème(s) | À rédiger | Statut | Commit |
|---|---|---|---|---|---|
| 1 | CR | Histoire, géographie et culture | 31 | ☑ thème complet (49/49) | |
| 2 | CR | Système institutionnel et politique | 35 | ☑ thème complet (50/50) | |
| 3 | CR | Principes et valeurs de la République | 30 | ☑ thème complet (40/40) | |
| 4 | CR | Droits et devoirs + Vivre dans la société française | 33 + 20 | ☑ **QCM CR complet (209/209)** | |
| 5 | CSP | Histoire, géographie et culture | 30 | ☑ thème complet (47/47) | |
| 6 | CSP | Système institutionnel et politique | 36 | ☑ thème complet (46/46) | |
| 7 | CSP | Principes et valeurs de la République | 32 | ☑ thème complet (37/37) | |
| 8 | CSP | Droits et devoirs + Vivre dans la société française | 28 + 21 | ☑ **QCM CSP complet (191/191)** | |

**Total à rédiger : 296 questions** (149 CR + 147 CSP) — **296 rédigées, 0 restante.** ✅

### Après le dernier lot

- [x] `check_questions_consistency.py` sans aucun avertissement de couverture
- [x] `QUESTIONS.md` régénéré (**738 questions**)
- [x] Effectifs mis à jour dans `CLAUDE.md` et `README.md`
- [x] Cibles par défaut des succès « Tour complet » alignées sur les effectifs réels
      (338 / 289 / 271 — la valeur affichée est de toute façon résolue au runtime)

### Effectifs finaux par QCM et par thème

| Thème | NAT | CR | CSP | Mises en situation (communes) |
|---|---|---|---|---|
| Principes et valeurs de la République | 39 | 40 | 37 | 20 |
| Système institutionnel et politique | 55 | 50 | 46 | 20 |
| Droits et devoirs | 37 | 38 | 30 | 20 |
| Histoire, géographie et culture | 83 | 49 | 47 | 0 |
| Vivre dans la société française | 44 | 32 | 31 | 20 |
| **Total connaissances** | **258** | **209** | **191** | **80** |
| **Questions tirables (avec les mises en situation)** | **338** | **289** | **271** | |

Chaque QCM tire donc bien 40 questions par examen (28 connaissances + 12 mises en situation),
vérifié par simulation sur 50 examens consécutifs par QCM, sans aucun doublon intra-examen et avec
une couverture complète du vivier par la rotation.

### Note de contenu

La liste officielle CSP comporte **deux fois la même question** sur l'obligation scolaire
(ids 3186 et 3189, qui ne diffèrent que par un trait d'union). Les deux entrées portent
volontairement le **même jeu de réponses** : c'est ce que détecte et impose
`check_questions_consistency.py`.

---

## 6. Recette manuelle (sur l'APK produit par la CI)

- [ ] Sélectionner chaque examen sur l'accueil : les questions tirées appartiennent bien à sa liste
- [ ] Mettre un examen CR en pause, revenir à l'accueil, reprendre → toujours en CR
- [ ] L'historique affiche l'examen passé sur chaque ligne, l'export aussi
- [ ] Les 3 sections d'entraînement ont des barres `X/total` indépendantes
- [ ] Les succès d'entraînement CR et CSP se débloquent séparément de ceux de la naturalisation
- [ ] **Non-régression** : après mise à jour d'une installation existante, l'historique, les succès
      et la progression d'entraînement « naturalisation » sont conservés
