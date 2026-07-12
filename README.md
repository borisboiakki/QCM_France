# QCM France

Application Android de préparation à l'examen civique de naturalisation française.

---

## Fonctionnalités

### Examen simulé
- **40 questions** tirées aléatoirement : **28 questions de connaissances** + **12 questions de mise en situation**, dans une base de 318 questions officielles (258 connaissances + 60 mises en situation)
- **Questions de mise en situation** — cas concrets de la vie quotidienne (voisinage, travail, discrimination, urgence…) où il faut choisir la réaction la plus appropriée, en plus des questions de connaissances classiques
- **Tirage proportionnel** par thème pour respecter la répartition de l'examen réel, séparément pour les questions de connaissances et de mise en situation
- **Anti-répétition entre examens** — chaque couple thème/type cycle sur toutes ses questions (ordre mélangé et persisté) avant qu'une question ne puisse revenir, au lieu d'un tirage aléatoire indépendant à chaque examen
- **Chronomètre décompte 45 minutes** — affiché en rouge dans les 5 dernières minutes
- **Soumission automatique** à 00:00 si l'examen n'est pas terminé manuellement
- **Aucun feedback pendant l'examen** (règle officielle) — les réponses correctes ne sont révélées qu'à la fin
- **Son de sélection** — bip au choix d'une réponse (activable/désactivable dans les paramètres)
- **Pause et reprise** — sauvegarde l'état complet (questions, réponses, timer) et permet de reprendre plus tard, même après fermeture de l'application

### Mode S'entraîner
- **Choix du thème** parmi les 5 thèmes officiels — barre de progression `X/total` par thème
- **Toutes les questions du thème**, une par une, dans un ordre stable et cohérent
- **Avancement persisté** : reprendre un thème interrompu reprend exactement là où on s'était arrêté, même après fermeture de l'application
- **Feedback immédiat** : sélectionner une réponse puis cliquer **Confirmer** révèle la correction — la bonne réponse passe en vert, une mauvaise sélection en rouge
- **Explication** affichée après confirmation (si disponible)
- **Lien vers la source officielle** cliquable après confirmation, ouvrant le navigateur
- **Écran de fin de thème** avec bouton « Recommencer ce thème »
- **Réinitialisation globale** de la progression depuis les Paramètres

### Écran d'accueil
Rappel des règles officielles avant de commencer : nombre de questions, seuil de réussite, durée, format.  
Accès rapide à l'historique, aux paramètres et à l'aide (icône Info dans la barre du haut).  
Si un examen est en pause, bouton **Reprendre l'examen** (couleur secondaire). Démarrer un nouvel examen affiche une confirmation pour éviter d'écraser l'état sauvegardé.

### Écran de quiz
- Barre de progression (question N / 40)
- Énoncé de la question
- 4 propositions de réponse (boutons radio)
- Chronomètre `MM:SS` en haut à droite
- Bouton **Suivant** jusqu'à la dernière question, puis **Terminer**
- Bouton **Pause** (header) et bouton retour système — sauvegardent l'état et retournent à l'accueil

### Écran de résultats
- Score final `X / 40`
- Mention **RÉUSSI** (fond vert, score ≥ 32) ou **ÉCHOUÉ** (fond rouge, score < 32)
- Temps utilisé affiché
- Détail question par question : réponse donnée vs bonne réponse
- **Filtre "Erreurs (N)"** — chip pour n'afficher que les questions ratées (mauvaise réponse ou sans réponse) ; message spécifique si le score est parfait
- Bouton **Recommencer** pour relancer un nouvel examen
- Bouton **Exporter les résultats** — partage le rapport complet (détail par question) via l'intent Android standard

### Historique des résultats
- Liste de tous les examens passés (date, score, durée, mention)
- Icône de partage sur chaque entrée pour exporter le résumé
- Bouton **Vider l'historique** avec confirmation

### Paramètres
- **Thème** : Système (par défaut) / Clair / Sombre — persisté entre les sessions
- **Son de sélection** : activé/désactivé — persisté entre les sessions
- **Réinitialiser la progression** : efface l'avancement de tous les thèmes du mode entraînement (avec confirmation)
- **Réinitialiser le cycle de l'examen** : relance le tirage anti-répétition à zéro pour chaque thème (avec confirmation)

### Écran d'aide
Accessible via l'icône Info (barre du haut de l'accueil) :
- Rappel des règles officielles et de la répartition des thèmes
- Description des fonctionnalités de l'application
- 7 liens cliquables vers les ressources officielles (gouvernement, Légifrance, Conseil constitutionnel)

---

## Règles officielles de l'examen

| Règle | Valeur |
|---|---|
| Nombre de questions | 40 |
| Seuil de réussite | 32 / 40 (80 %) |
| Durée maximale | 45 minutes |
| Format | 1 seule bonne réponse parmi 4 |
| Feedback | Uniquement à l'écran résultat |

### Répartition des thèmes (tirage stratifié)

Le total par thème correspond à l'examen officiel (6/9/6/13/6 = 40) ; seule sa composition
interne change entre questions de connaissances et de mise en situation. Le thème « Histoire,
géographie et culture » reste 100 % connaissances (aucune mise en situation adaptée à ce thème).

| Thème | Connaissances (base) | Mise en situation (base) | Tirées à l'examen |
|---|---|---|---|
| Principes et valeurs de la République | 39 | 15 | 3 + 3 = 6 |
| Système institutionnel et politique | 55 | 15 | 6 + 3 = 9 |
| Droits et devoirs | 37 | 15 | 3 + 3 = 6 |
| Histoire, géographie et culture | 83 | 0 | 13 + 0 = 13 |
| Vivre dans la société française | 44 | 15 | 3 + 3 = 6 |
| **Total** | **258** | **60** | **28 + 12 = 40** |

---

## Architecture

L'application suit le pattern **MVVM + Clean Architecture** avec un flux de données unidirectionnel.

```
UI (Compose)  ──événements──►  ViewModel  ──suspend──►  Repository  ──suspend──►  Room (SQLite)
              ◄──StateFlow──               ◄────────────────────────────────────────
```

### Couches

```
app/src/main/java/com/example/qcmfrance/
│
├── data/                          Couche données (indépendante de l'UI)
│   ├── model/
│   │   ├── Question.kt            Entité Room : id, theme, text, optionA-D,
│   │   │                          correctAnswer, explanation, source, isSituation
│   │   ├── QuizResult.kt          Entité Room : id, date, score, passed, duration
│   │   ├── PausedQuiz.kt          Entité Room singleton : état sérialisé (questions, réponses, timer)
│   │   ├── TrainingProgress.kt    Entité Room : PK=theme, currentIndex (point de reprise par thème)
│   │   └── ExamCycle.kt           Entité Room : PK=theme, permutation d'ids (JSON) + curseur (anti-répétition examen)
│   ├── db/
│   │   ├── QuestionDao.kt         DAO Room : getAllByTheme, getIdsByTheme(theme, isSituation), getByIds, countByTheme, insertAll, count
│   │   ├── QuizResultDao.kt       DAO Room : getAll (Flow), insert, deleteAll
│   │   ├── PausedQuizDao.kt       DAO Room : save (REPLACE), get, observe (Flow), delete
│   │   ├── TrainingProgressDao.kt DAO Room : save (REPLACE), get, observeAll (Flow), clear
│   │   ├── ExamCycleDao.kt        DAO Room : save (REPLACE), get, clear
│   │   ├── Converters.kt          @TypeConverter List<String> ↔ JSON String
│   │   └── AppDatabase.kt         Base Room v8 + migrations 1→2→3→4→5→6→7→8
│   └── repository/
│       ├── QuestionRepository.kt  seedIfNeeded (2 fichiers JSON) + tirage stratifié 28 connaissances + 12 mise en situation, cyclé par thème/type (exam_cycle), liste des thèmes
│       ├── TrainingRepository.kt  Questions par thème (ordre stable), avancement par thème
│       ├── HistoryRepository.kt   Sauvegarde et récupération de l'historique des résultats
│       ├── SettingsRepository.kt  DataStore : ThemeMode + soundEnabled
│       └── PausedQuizRepository.kt  Sérialisation Gson : save / load / clear / observeHasPaused
│
├── di/
│   └── AppModule.kt               Module Hilt : fournit AppDatabase et tous les DAOs
│
└── ui/                            Couche présentation
    ├── viewmodel/
    │   ├── QuizViewModel.kt       QuizUiState, timerJob (cancellable), pauseQuiz/resumeQuiz
    │   ├── TrainingViewModel.kt   TrainingUiState + themeProgress, startTheme/confirmAnswer/next/restart/reset
    │   ├── QuestionExt.kt         Helper partagé withShuffledOptions() (examen + entraînement)
    │   ├── HomeViewModel.kt       hasPausedQuiz : StateFlow<Boolean> (Flow réactif depuis Room)
    │   ├── HistoryViewModel.kt    Flow<List<QuizResult>>, clearHistory()
    │   └── SettingsViewModel.kt   themeMode + soundEnabled StateFlow
    ├── navigation/
    │   └── NavGraph.kt            8 routes : home / quiz / result / history / settings / help / training_themes / training
    ├── screen/
    │   ├── HomeScreen.kt          Accueil : règles, boutons (examen + entraînement), icône Aide
    │   ├── QuizScreen.kt          Examen : question N/40, options, timer, Pause, BackHandler, son
    │   ├── ResultScreen.kt        Résultat : score, mention, filtre erreurs, export
    │   ├── HistoryScreen.kt       Historique : liste, export par résultat, vider
    │   ├── SettingsScreen.kt      Paramètres : thème, toggle son, réinitialiser entraînement, réinitialiser cycle examen
    │   ├── HelpScreen.kt          Aide : guide utilisateur + 7 liens officiels cliquables
    │   ├── TrainingThemesScreen.kt  Sélection du thème + barre X/total par thème
    │   └── TrainingScreen.kt      Question d'entraînement, feedback immédiat, explication + lien source
    ├── utils/
    │   └── ResultExporter.kt      Partage texte via Intent.ACTION_SEND
    └── theme/
        ├── Color.kt, Type.kt      Palette et typographie
        └── Theme.kt               Thème Material 3 (dynamique sur Android 12+)
```

### Gestion de l'état — `QuizUiState`

Tout l'état du quiz est contenu dans un seul `data class` exposé via `StateFlow` :

```kotlin
data class QuizUiState(
    val questions: List<Question>,   // 40 questions tirées
    val currentIndex: Int,           // 0..39
    val answers: Map<Int, String>,   // questionId → "A" | "B" | "C" | "D"
    val isFinished: Boolean,
    val score: Int,                  // calculé à la soumission
    val passed: Boolean,             // score >= 32
    val remainingSeconds: Int,       // 2700 → 0
    val timerExpired: Boolean,
    val isLoading: Boolean
)
```

Les composables Compose sont **stateless** : ils reçoivent l'état et des lambdas d'événements, sans logique interne.

### Timer

Le timer tourne dans `viewModelScope` indépendamment de l'écran affiché :

```
startQuiz()
    └─ runTimer()  (coroutine)
           ├─ delay(1000) × N  →  remainingSeconds--
           └─ à 0 s : submitQuiz()  →  isFinished = true
                                         └─ LaunchedEffect dans NavGraph
                                                └─ navigate(result)
```

### Seed de la base de données

Les 258 questions de connaissances (`res/raw/questions.json`) et les 60 questions de mise en
situation (`res/raw/situational_questions.json`) sont insérées dans Room **une seule fois**, au
premier lancement, dans `QuestionRepository.seedIfNeeded()` (vérification `count() == 0`),
appelée par le mode examen comme par le mode entraînement. Les lancements suivants utilisent
directement la base SQLite.

### Cycle de tirage de l'examen (anti-répétition)

Le tirage n'utilise pas `ORDER BY RANDOM()` à chaque examen (ce qui permettrait de retirer les mêmes questions d'un examen à l'autre). Chaque **couple thème + type** (connaissances ou mise en situation) a une permutation persistée de ses ids (table `exam_cycle`) et un curseur : chaque nouvel examen consomme la suite de la permutation, garantissant que toutes les questions d'un thème/type sont utilisées une fois avant qu'une répétition ne survienne. Quand un cycle boucle, une nouvelle permutation est générée pour le tour suivant. Ce cycle est indépendant de la pause/reprise : il n'avance qu'au lancement d'un nouvel examen.

### Navigation

```
[HomeScreen]
     │  "Commencer l'examen" → startQuiz() + navigate(quiz)
     │  "Reprendre l'examen" → resumeQuiz() + navigate(quiz)   (visible si pause sauvegardée)
     │  "S'entraîner par thème" → navigate(training_themes)
     │  Icône Historique → navigate(history)
     │  Icône Paramètres → navigate(settings)
     │  Icône Aide → navigate(help)
     │
     ├──► [TrainingThemesScreen]   Liste des 5 thèmes + barre X/total
     │         │  Choisir un thème → startTheme() + navigate(training)
     │         ▼
     │    [TrainingScreen]   Question d'entraînement, feedback immédiat
     │         │  Sélectionner → "Confirmer" → révèle vert/rouge + explication + source
     │         │  "Suivant" → question suivante (avancement persisté)
     │         │  "Terminer" (dernière question) → écran de fin → retour aux thèmes
     │         │  Retour → popBackStack(training_themes)
     │
     ▼
[QuizScreen]  ←── timer démarre (ou reprend)
     │  "Terminer" → submitQuiz()
     │  ou timer expiré → submitQuiz() automatique
     │  "Pause" / retour système → pauseQuiz() + popBackStack(home)
     │  isFinished = true → LaunchedEffect → navigate(result)
     ▼
[ResultScreen]
     │  "Recommencer" → restartQuiz() + popBackStack(home)
     └──────────────────────────────────────────────────────►  [HomeScreen]
```

---

## Stack technique

| Composant | Technologie | Version |
|---|---|---|
| Langage | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Architecture | MVVM + StateFlow | — |
| Base de données | Room (SQLite) | 2.6.1 |
| Préférences | DataStore Preferences | 1.1.1 |
| Injection | Hilt (Dagger) | 2.52 |
| Navigation | Navigation Compose | 2.8.5 |
| JSON | Gson | 2.10.1 |
| Build | Gradle Kotlin DSL + KSP | 8.13 / 2.0.21-1.0.27 |
| SDK cible | Android 8+ (API 26+) | compileSdk 35 |

---

## Installation & build

### Prérequis
- JDK 17 ou supérieur
- Android SDK (compileSdk 35, build-tools 35)

### Lancer le build debug
```bash
./gradlew assembleDebug
```

### Installer sur un device/émulateur connecté
```bash
./gradlew installDebug
```

### Vérifications
```bash
./gradlew lint       # analyse statique
./gradlew test       # tests unitaires
```

L'APK debug se trouve dans `app/build/outputs/apk/debug/`.

---

## Sources officielles

Les 258 questions de connaissances sont issues des documents officiels du Ministère de l'Intérieur :

| Document | Description |
|---|---|
| **Livret du citoyen** | Référentiel officiel des connaissances civiques pour la naturalisation |
| **Charte des droits et devoirs du citoyen français** | Document signé lors de la cérémonie de naturalisation |
| **Questions officielles de l'examen de naturalisation 2025** | Base de questions publiée par les services de l'État |
| **Jeu de données data.gouv.fr** | Dataset public officiel des QCM de naturalisation |

Les 60 questions de mise en situation sont sourcées individuellement vers les textes de référence
correspondants (Code pénal et Code du travail via Légifrance, Défenseur des droits,
service-public.fr, education.gouv.fr, Assurance Maladie, etc.) — voir le champ « Source » de
chaque question dans `QUESTIONS.md`.

---

## Ressources utiles pour les utilisateurs

Liens disponibles depuis l'écran **Aide** de l'application :

| Ressource | Lien |
|---|---|
| Informations générales sur l'examen civique | https://formation-civique.interieur.gouv.fr/examen-civique/informations-g%C3%A9n%C3%A9rales-sur-lexamen-civique/ |
| Tests complémentaires en ligne (Ensemble en France) | https://www.ensemble-en-france.org/quiz-examen-civique-gratuit-debutant/ |
| Fiche d'information sur l'examen | https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/lexamen-civique-pour-demande-de-naturalisation-ou-de-reintegration-dans-nationalite-francaise.html |
| Livret du citoyen | https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/livret-du-citoyen.html |
| Charte des droits et devoirs du citoyen français | https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/charte-des-droits-et-devoirs-du-citoyen-francais.html |
| Déclaration des droits de l'homme et du citoyen (1789) | https://www.conseil-constitutionnel.fr/le-bloc-de-constitutionnalite/declaration-des-droits-de-l-homme-et-du-citoyen-de-1789 |
| Constitution française (1958) | https://www.legifrance.gouv.fr/loda/id/JORFTEXT000000571356/ |

---

## Structure des fichiers source

```
QCM_France/
├── .github/workflows/
│   ├── build.yml              CI — build APK debug sur push/PR vers main
│   └── release.yml            Release — build + publication sur tag v.N.N.N
├── scripts/
│   └── generate_questions_md.py   Génère QUESTIONS.md depuis questions.json + situational_questions.json
├── LICENSE                    Licence MIT
├── QUESTIONS.md               Liste des 318 questions : 258 connaissances + 60 mise en situation (généré automatiquement)
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/qcmfrance/
│       │   ├── MainActivity.kt
│       │   ├── QcmFranceApplication.kt
│       │   ├── data/
│       │   │   ├── db/        AppDatabase.kt (v8)  QuestionDao.kt  QuizResultDao.kt
│       │   │   │              PausedQuizDao.kt  TrainingProgressDao.kt  ExamCycleDao.kt  Converters.kt
│       │   │   ├── model/     Question.kt  QuizResult.kt  PausedQuiz.kt  TrainingProgress.kt  ExamCycle.kt
│       │   │   └── repository/QuestionRepository.kt  TrainingRepository.kt
│       │   │                  HistoryRepository.kt  SettingsRepository.kt  PausedQuizRepository.kt
│       │   ├── di/            AppModule.kt
│       │   └── ui/
│       │       ├── navigation/NavGraph.kt
│       │       ├── screen/    HomeScreen.kt  QuizScreen.kt  ResultScreen.kt
│       │       │              HistoryScreen.kt  SettingsScreen.kt  HelpScreen.kt
│       │       │              TrainingThemesScreen.kt  TrainingScreen.kt
│       │       ├── utils/     ResultExporter.kt
│       │       ├── viewmodel/ QuizViewModel.kt  TrainingViewModel.kt  QuestionExt.kt
│       │       │              HomeViewModel.kt  HistoryViewModel.kt  SettingsViewModel.kt
│       │       └── theme/     Theme.kt  Color.kt  Type.kt
│       └── res/
│           ├── mipmap-*/      Icônes adaptatives (fond bleu tricolore, texte QCM)
│           ├── raw/           questions.json (258, seed)  situational_questions.json (60, seed)
│           └── values/        strings.xml  themes.xml  colors.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    ├── libs.versions.toml
    └── wrapper/gradle-wrapper.properties
```
