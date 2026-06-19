# QCM France

Application Android de préparation à l'examen civique de naturalisation française.

---

## Fonctionnalités

### Examen simulé
- **40 questions** tirées aléatoirement dans une base de 258 questions officielles
- **Tirage proportionnel** par thème pour respecter la répartition de l'examen réel
- **Chronomètre décompte 45 minutes** — affiché en rouge dans les 5 dernières minutes
- **Soumission automatique** à 00:00 si l'examen n'est pas terminé manuellement
- **Aucun feedback pendant l'examen** (règle officielle) — les réponses correctes ne sont révélées qu'à la fin
- **Son de sélection** — bip au choix d'une réponse (activable/désactivable dans les paramètres)

### Écran d'accueil
Rappel des règles officielles avant de commencer : nombre de questions, seuil de réussite, durée, format.  
Accès rapide à l'historique et aux paramètres.

### Écran de quiz
- Barre de progression (question N / 40)
- Énoncé de la question
- 4 propositions de réponse (boutons radio)
- Chronomètre `MM:SS` en haut à droite
- Bouton **Suivant** jusqu'à la dernière question, puis **Terminer**

### Écran de résultats
- Score final `X / 40`
- Mention **RÉUSSI** (fond vert, score ≥ 32) ou **ÉCHOUÉ** (fond rouge, score < 32)
- Temps utilisé affiché
- Détail question par question : réponse donnée vs bonne réponse
- Bouton **Recommencer** pour relancer un nouvel examen
- Bouton **Exporter les résultats** — partage le rapport complet (détail par question) via l'intent Android standard

### Historique des résultats
- Liste de tous les examens passés (date, score, durée, mention)
- Icône de partage sur chaque entrée pour exporter le résumé
- Bouton **Vider l'historique** avec confirmation

### Paramètres
- **Thème** : Système (par défaut) / Clair / Sombre — persisté entre les sessions
- **Son de sélection** : activé/désactivé — persisté entre les sessions

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

| Thème | Questions dans la base | Tirées à l'examen |
|---|---|---|
| Principes et valeurs de la République | 39 | 6 |
| Système institutionnel et politique | 55 | 9 |
| Droits et devoirs | 37 | 6 |
| Histoire, géographie et culture | 83 | 13 |
| Vivre dans la société française | 44 | 6 |
| **Total** | **258** | **40** |

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
│   │   └── Question.kt            Entité Room : id, theme, text, optionA-D,
│   │                              correctAnswer, correctAnswers (JSON), explanation
│   ├── db/
│   │   ├── QuestionDao.kt         DAO Room : getRandomByTheme, insertAll, count
│   │   └── AppDatabase.kt         Base Room + seed automatique au 1er lancement
│   └── repository/
│       └── QuestionRepository.kt  Tirage stratifié 6-9-6-13-6 = 40 questions
│
├── di/
│   └── AppModule.kt               Module Hilt : fournit AppDatabase, QuestionDao,
│                                  QuestionRepository en @Singleton
│
└── ui/                            Couche présentation
    ├── viewmodel/
    │   └── QuizViewModel.kt       État centralisé (QuizUiState) + logique métier
    ├── navigation/
    │   └── NavGraph.kt            Graph de navigation : home → quiz → result
    ├── screen/
    │   ├── HomeScreen.kt          Écran d'accueil
    │   ├── QuizScreen.kt          Écran de quiz
    │   └── ResultScreen.kt        Écran de résultats
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

Les 258 questions sont embarquées dans `res/raw/questions.json` et insérées dans Room **une seule fois**, au premier lancement, via un `RoomDatabase.Callback.onCreate()`. Les lancements suivants utilisent directement la base SQLite.

### Navigation

```
[HomeScreen]
     │  "Commencer l'examen" → startQuiz() + navigate(quiz)
     ▼
[QuizScreen]  ←── timer démarre
     │  "Terminer" → submitQuiz()
     │  ou timer expiré → submitQuiz() automatique
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
| Build | Gradle Kotlin DSL + KSP | 8.11.1 / 2.0.21-1.0.27 |
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

Les 258 questions sont issues des documents officiels du Ministère de l'Intérieur :

| Document | Description |
|---|---|
| **Livret du citoyen** | Référentiel officiel des connaissances civiques pour la naturalisation |
| **Charte des droits et devoirs du citoyen français** | Document signé lors de la cérémonie de naturalisation |
| **Questions officielles de l'examen de naturalisation 2025** | Base de questions publiée par les services de l'État |
| **Jeu de données data.gouv.fr** | Dataset public officiel des QCM de naturalisation |

---

## Structure des fichiers source

```
QCM_France/
├── .github/workflows/
│   ├── build.yml              CI — build APK debug sur push/PR vers main
│   └── release.yml            Release — build + publication sur tag v.N.N.N
├── scripts/
│   └── generate_questions_md.py   Génère QUESTIONS.md depuis questions.json
├── LICENSE                    Licence MIT
├── QUESTIONS.md               Liste des 258 questions (généré automatiquement)
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/qcmfrance/
│       │   ├── MainActivity.kt
│       │   ├── QcmFranceApplication.kt
│       │   ├── data/
│       │   │   ├── db/        AppDatabase.kt  QuestionDao.kt  QuizResultDao.kt  Converters.kt
│       │   │   ├── model/     Question.kt  QuizResult.kt
│       │   │   └── repository/QuestionRepository.kt  QuizResultRepository.kt  SettingsRepository.kt
│       │   ├── di/            AppModule.kt
│       │   └── ui/
│       │       ├── navigation/NavGraph.kt
│       │       ├── screen/    HomeScreen.kt  QuizScreen.kt  ResultScreen.kt
│       │       │              HistoryScreen.kt  SettingsScreen.kt
│       │       ├── utils/     ResultExporter.kt
│       │       ├── viewmodel/ QuizViewModel.kt  HistoryViewModel.kt  SettingsViewModel.kt
│       │       └── theme/     Theme.kt  Color.kt  Type.kt
│       └── res/
│           ├── mipmap-*/      Icônes adaptatives (fond bleu tricolore, texte QCM)
│           ├── raw/           questions.json (258 questions, seed)
│           └── values/        strings.xml  themes.xml  colors.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    ├── libs.versions.toml
    └── wrapper/gradle-wrapper.properties
```
