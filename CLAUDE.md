# QCM France — Android App

Application Android de préparation à l'examen civique de naturalisation française.

---

## Règles officielles de l'examen

| Règle | Valeur |
|---|---|
| Nombre de questions | **40** tirées aléatoirement dans la base |
| Seuil de réussite | **32/40 minimum (80 %)** |
| Durée maximale | **45 minutes** (chronomètre décompte affiché) |
| Format | Une seule bonne réponse parmi 4 propositions |
| Support | Numérique (cette app) |

### Les 5 thèmes officiels et leur répartition dans la base

| # | Thème officiel | Questions dans la base | Tirage proportionnel sur 40 |
|---|---|---|---|
| 1 | Principes et valeurs de la République | 39 | ~6 |
| 2 | Système institutionnel et politique | 55 | ~9 |
| 3 | Droits et devoirs | 37 | ~6 |
| 4 | Histoire, géographie et culture | 83 | ~13 |
| 5 | Vivre dans la société française | 44 | 6 |
| | **Total** | **258** | **40 (arrondi au plus proche, ajusté à 40)** |

**Stratégie de tirage :** tirage proportionnel par thème (stratified sampling) pour garantir que chaque thème est représenté, puis ajustement pour atteindre exactement 40. L'ordre des questions est ensuite mélangé.

### Calcul du tirage (implémentation)

```kotlin
val TOTAL_QUESTIONS = 40
val themeCounts = mapOf(
    "Principes et valeurs de la République" to 6,
    "Système institutionnel et politique"   to 9,
    "Droits et devoirs"                     to 6,
    "Histoire, géographie et culture"       to 13,
    "Vivre dans la société française"       to 6,
)
// Total = 40. Dernière thème ajustée si arrondi != 40.
```

**Anti-répétition entre examens :** le tirage n'utilise pas `ORDER BY RANDOM()` à chaque appel (ce qui permettrait à un thème de retirer les mêmes questions d'un examen à l'autre). À la place, chaque thème a une permutation persistée de ses ids (table `exam_cycle`) et un curseur ; chaque examen consomme la suite de la permutation. Toutes les questions d'un thème sont donc utilisées une fois avant qu'une répétition ne survienne ; quand un thème boucle, une nouvelle permutation est générée pour le tour suivant. Voir `QuestionRepository.drawIdsFromCycle()` et la section « Cycle de tirage de l'examen » plus bas.

### Chronomètre

- Durée : 45 minutes = 2 700 secondes
- Le timer tourne en continu dans le ViewModel avec `viewModelScope`
- Affichage `MM:SS` en rouge dans les 5 dernières minutes
- À 00:00 : soumission automatique des réponses données, les non-répondues comptent comme fausses

---

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.x |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture (UI → ViewModel → Repository → Room) |
| Base de données | Room (SQLite) |
| Injection de dépendances | Hilt (Dagger) |
| Coroutines / Flow | Kotlin Coroutines + StateFlow |
| Navigation | Navigation Compose |
| Build system | Gradle Kotlin DSL (`.kts`) |

---

## Versions cibles

```kotlin
// app/build.gradle.kts
compileSdk = 35
minSdk     = 26          // Android 8 — couvre >95 % des devices actifs
targetSdk  = 35
```

Kotlin : `2.0.21` | AGP : `8.13.2` | KSP : `2.0.21-1.0.27` | Gradle : `8.13`

---

## Structure du projet

```
QCM_France/
├── .github/
│   └── workflows/
│       ├── build.yml                            CI — build APK debug sur push/PR vers main
│       └── release.yml                          Release — build + publication sur tag v.N.N.N
│                                                (supporte aussi workflow_dispatch avec input version)
├── scripts/
│   └── generate_questions_md.py                 Génère QUESTIONS.md depuis questions.json
├── LICENSE                                      Licence MIT
├── QUESTIONS.md                                 Liste des 258 questions (généré par release)
├── AUDIO_CREDITS.md                             Sources/licences audio + remplacement des placeholders
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/qcmfrance/
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt           Room @Database v6 + migrations 1→2→3→4→5→6
│   │   │   │   │   ├── Converters.kt            @TypeConverter List<String> ↔ JSON String
│   │   │   │   │   ├── QuestionDao.kt           @Dao : getAllByTheme, getIdsByTheme, getByIds, countByTheme, insertAll, count
│   │   │   │   │   ├── QuizResultDao.kt         @Dao : getAll (Flow), insert, deleteAll
│   │   │   │   │   ├── PausedQuizDao.kt         @Dao : save (REPLACE), get, delete
│   │   │   │   │   ├── TrainingProgressDao.kt   @Dao : save (REPLACE), get, observeAll (Flow), clear
│   │   │   │   │   └── ExamCycleDao.kt          @Dao : save (REPLACE), get, clear
│   │   │   │   ├── model/
│   │   │   │   │   ├── Question.kt              @Entity Room
│   │   │   │   │   ├── QuizResult.kt            @Entity Room : id, date, score, passed, duration
│   │   │   │   │   ├── PausedQuiz.kt            @Entity Room : singleton (PK=1), état sérialisé JSON
│   │   │   │   │   ├── TrainingProgress.kt      @Entity Room : PK=theme, currentIndex (point de reprise)
│   │   │   │   │   └── ExamCycle.kt             @Entity Room : PK=theme, permutation d'ids (JSON) + curseur
│   │   │   │   └── repository/
│   │   │   │       ├── QuestionRepository.kt    seedIfNeeded + tirage stratifié 6-9-6-13-6 cyclé (exam_cycle), themes
│   │   │   │       ├── HistoryRepository.kt     sauvegarde et récupération de l'historique
│   │   │   │       ├── SettingsRepository.kt    DataStore : ThemeMode + soundEnabled
│   │   │   │       ├── PausedQuizRepository.kt  save/load/clear + PausedQuizState (Gson)
│   │   │   │       └── TrainingRepository.kt    questions par thème (ordre stable), avancement par thème
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                 Hilt @Module (AppDatabase, DAOs)
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt              8 routes : home/quiz/result/history/settings/help/training_themes/training
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt            titre, règles, Reprendre (si pause), S'entraîner par thème, AlertDialog, icône Aide
│   │   │   │   │   ├── QuizScreen.kt            question N/40, options, timer, bouton Pause, BackHandler, son
│   │   │   │   │   ├── ResultScreen.kt          score, RÉUSSI/ÉCHOUÉ, temps, filtre erreurs (FilterChip), détail, export, musique de fin (MediaPlayer)
│   │   │   │   │   ├── HistoryScreen.kt         liste des résultats, export par résultat, vider
│   │   │   │   │   ├── SettingsScreen.kt        thème (Système/Clair/Sombre), toggle son, réinitialiser l'entraînement, réinitialiser le cycle de l'examen
│   │   │   │   │   ├── HelpScreen.kt            guide utilisateur + 7 liens officiels cliquables
│   │   │   │   │   ├── TrainingThemesScreen.kt  sélection du thème + barre d'avancement X/total par thème
│   │   │   │   │   └── TrainingScreen.kt        question du thème, feedback immédiat (vert/rouge), explication + lien source
│   │   │   │   ├── utils/
│   │   │   │   │   └── ResultExporter.kt        partage texte via Intent.ACTION_SEND
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── QuizViewModel.kt         QuizUiState, timerJob (cancellable), pauseQuiz/resumeQuiz, scoring, resetExamCycle
│   │   │   │   │   ├── TrainingViewModel.kt     TrainingUiState, themeProgress, startTheme/selectAnswer/confirmAnswer/next/restart/reset
│   │   │   │   │   ├── QuestionExt.kt           helper partagé withShuffledOptions() (examen + entraînement)
│   │   │   │   │   ├── HomeViewModel.kt         hasPausedQuiz : StateFlow<Boolean>
│   │   │   │   │   ├── HistoryViewModel.kt      Flow<List<QuizResult>>, clearHistory()
│   │   │   │   │   └── SettingsViewModel.kt     themeMode + soundEnabled StateFlow
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt                 Material 3 dynamique, accepte ThemeMode
│   │   │   │       ├── Color.kt
│   │   │   │       └── Type.kt
│   │   │   ├── QcmFranceApplication.kt          @HiltAndroidApp
│   │   │   └── MainActivity.kt                  @AndroidEntryPoint, collecte ThemeMode
│   │   └── res/
│   │       ├── mipmap-*/                        Icônes adaptatives (fond bleu tricolore)
│   │       ├── raw/
│   │       │   ├── questions.json               258 questions (seed)
│   │       │   ├── marseillaise.ogg             musique si examen réussi (domaine public — voir AUDIO_CREDITS.md)
│   │       │   └── marche_funebre.ogg           musique si examen échoué (domaine public — voir AUDIO_CREDITS.md)
│   │       └── values/
│   │           ├── strings.xml
│   │           ├── themes.xml
│   │           └── colors.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle.properties                            android.useAndroidX=true, android.nonTransitiveRClass=true, org.gradle.jvmargs=-Xmx4g
├── settings.gradle.kts
├── gradlew / gradlew.bat                        Gradle wrapper 8.11.1
└── gradle/
    ├── libs.versions.toml                       Version catalog complet
    └── wrapper/
        └── gradle-wrapper.properties
```

---

## Modèle de données

### Entité Room — `Question`

```kotlin
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: Int,
    val theme: String,              // L'un des 5 thèmes officiels
    val text: String,               // Énoncé de la question
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,      // "A", "B", "C" ou "D"
    val correctAnswers: List<String>, // toutes les réponses officiellement acceptées
    val explanation: String = ""      // stocké en JSON String dans Room via Converters.kt
)
```

`correctAnswers` est sérialisé en JSON String dans SQLite par `Converters.kt` (`@TypeConverter`).

### Entité Room — `PausedQuiz`

```kotlin
@Entity(tableName = "paused_quiz")
data class PausedQuiz(
    @PrimaryKey val id: Int = 1,           // singleton — un seul examen en pause à la fois
    val questionsJson: String,             // Gson List<Question> avec options déjà mélangées
    val answersJson: String,               // Gson Map<Int,String> (questionId → lettre)
    val currentIndex: Int,
    val remainingSeconds: Int,
    val savedAt: Long = System.currentTimeMillis()
)
```

INSERT OR REPLACE sur PK=1 garantit qu'il n'y a jamais plus d'une ligne. La table est créée par `MIGRATION_3_4`.

### Entité Room — `TrainingProgress`

```kotlin
@Entity(tableName = "training_progress")
data class TrainingProgress(
    @PrimaryKey val theme: String,         // un des 5 thèmes officiels — une ligne par thème
    val currentIndex: Int,                 // index 0-based de la prochaine question = nb de questions complétées
    val updatedAt: Long = System.currentTimeMillis()
)
```

Avancement du **mode entraînement**. `currentIndex` sert de point de reprise et de valeur « X » de la barre `X/total`. Thème terminé quand `currentIndex >= total`. La table est créée par `MIGRATION_4_5` (BDD passée en v5). Réinitialisation globale via `TrainingProgressDao.clear()` (bouton dans les Paramètres).

### Entité Room — `ExamCycle`

```kotlin
@Entity(tableName = "exam_cycle")
data class ExamCycle(
    @PrimaryKey val theme: String,   // un des 5 thèmes officiels — une ligne par thème
    val orderJson: String,           // Gson List<Int> — permutation des ids du thème
    val cursor: Int                  // index de la prochaine question à tirer dans la permutation
)
```

Cycle de tirage de l'**examen** (pas l'entraînement). Voir « Cycle de tirage de l'examen » ci-dessous pour la logique complète. La table est créée par `MIGRATION_5_6` (BDD passée en v6). Réinitialisation via `QuestionRepository.resetExamCycle()` → `ExamCycleDao.clear()` (bouton « Réinitialiser le cycle de l'examen » dans les Paramètres).

### Cycle de tirage de l'examen

`QuestionRepository.drawIdsFromCycle(theme, count)` remplace l'ancien tirage `ORDER BY RANDOM()` :

1. Charge la permutation persistée (`exam_cycle.orderJson`) et son curseur pour le thème ; si absente, ou si l'ensemble des ids ne correspond plus (questions ajoutées/supprimées), génère une nouvelle permutation aléatoire et repart du curseur 0.
2. Prend les `count` ids suivants à partir du curseur, en avançant le curseur.
3. Si la permutation est épuisée avant d'avoir pris `count` ids (fin d'un tour), génère une nouvelle permutation de l'ensemble des ids du thème pour le tour suivant — en plaçant les ids déjà pris dans ce tirage en fin de liste, pour ne pas les retirer immédiatement dans le même examen.
4. Persiste la permutation (éventuellement renouvelée) et le nouveau curseur.

Résultat : toutes les questions d'un thème sont utilisées une fois avant qu'une répétition ne survienne d'un examen à l'autre. Indépendant du flux pause/reprise (`PausedQuiz`) : le cycle n'avance qu'au lancement d'un nouvel examen (`QuizViewModel.startQuiz()` → `drawStratifiedQuestions()`), jamais pendant une reprise.

---

## Mode « S'entraîner » (entraînement)

Mode complémentaire à l'examen, orienté apprentissage — l'inverse UX de l'examen.

| Aspect | Examen | Entraînement |
|---|---|---|
| Questions | 40 tirées aléatoirement (tous thèmes) | toutes les questions d'**un** thème choisi |
| Ordre | mélangé | **fixe** (`ORDER BY id`) pour une reprise cohérente |
| Chronomètre | 45 min | aucun |
| Feedback | uniquement à la fin | **immédiat** après chaque réponse |
| Source | — | **lien cliquable** (`Question.source`) + explication, dans tous les cas |
| Avancement | pause/reprise (1 examen) | **par thème**, persisté à chaque question |

- **Flux** : Accueil → « S'entraîner par thème » → `TrainingThemesScreen` (liste des 5 thèmes + barre `X/total`) → choix d'un thème → `TrainingScreen`.
- **Persistance** : `TrainingViewModel.next()` enregistre `currentIndex` après chaque question via `TrainingRepository.saveProgress(theme, index)`. Aucun mécanisme « pause » nécessaire : un simple retour ne perd rien.
- **Feedback** : sélection (`selectAnswer`, modifiable) → bouton **« Confirmer »** (`confirmAnswer`) → `revealed=true`, l'option correcte passe en vert, une mauvaise réponse sélectionnée en rouge ; bloc « Bonne/Mauvaise réponse » + `explanation` (si non vide) + bouton « Voir la source » (`LocalUriHandler.openUri`). Tant que la réponse n'est pas confirmée, la correction reste cachée et la sélection peut être changée. Après confirmation, le bouton bas devient « Suivant »/« Terminer ».
- **Seed partagé** : `QuestionRepository.seedIfNeeded()` (extrait de `drawStratifiedQuestions()`) est appelé aussi par le chemin entraînement, pour le cas où l'utilisateur ouvre l'entraînement avant tout examen.
- **Option shuffling** : réutilise `withShuffledOptions()` (déplacé dans `ui/viewmodel/QuestionExt.kt`, partagé par les deux ViewModels).
- **Réinitialisation** : Paramètres → « Réinitialiser la progression » (AlertDialog de confirmation) → `TrainingViewModel.resetTraining()` → `TrainingRepository.resetAll()`.

---

### Format JSON de seed (`res/raw/questions.json`)

```json
[
  {
    "id": 1,
    "theme": "Principes et valeurs de la République",
    "text": "Complétez les paroles de la Marseillaise...",
    "optionA": "Le jour de gloire est arrivé",
    "optionB": "Vive la France et la liberté !",
    "optionC": "Aux armes, citoyens, formez vos bataillons !",
    "optionD": "Contre nous, la tyrannie est vaincue !",
    "correctAnswer": "A",
    "correctAnswers": ["Le jour de gloire est arrivé"],
    "explanation": "",
    "source": "https://fr.wikipedia.org/wiki/La_Marseillaise"
  }
]
```

> `correctAnswers` est un **tableau JSON** dans le fichier — Gson le désérialise en `List<String>`,
> Room le stocke en chaîne JSON via `Converters.kt`.

---

## DAO

```kotlin
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY id")
    suspend fun getAllByTheme(theme: String): List<Question>

    @Query("SELECT id FROM questions WHERE theme = :theme ORDER BY id")
    suspend fun getIdsByTheme(theme: String): List<Int>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int
}
```

---

## ViewModel — état du quiz

```kotlin
data class QuizUiState(
    val questions: List<Question> = emptyList(),   // 40 questions tirées
    val currentIndex: Int = 0,                     // 0..39
    val answers: Map<Int, String> = emptyMap(),    // questionId → lettre choisie ("A".."D")
    val isFinished: Boolean = false,
    val score: Int = 0,                            // calculé à la fin
    val passed: Boolean = false,                   // score >= 32
    // Timer
    val remainingSeconds: Int = 2700,              // 45 min = 2700 s
    val timerExpired: Boolean = false,
    val isLoading: Boolean = true                  // spinner pendant le chargement des questions
)
```

**Événements :** `SelectAnswer(letter)`, `NextQuestion`, `SubmitQuiz`, `RestartQuiz`, `PauseQuiz`, `ResumeQuiz`

**Logique du timer :**
```kotlin
private var timerJob: Job? = null   // stocké pour permettre l'annulation (pause)

private fun runTimer() {
    timerJob = viewModelScope.launch {
        while (_uiState.value.remainingSeconds > 0 && !_uiState.value.isFinished) {
            delay(1000L)
            _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
        }
        if (_uiState.value.remainingSeconds == 0 && !_uiState.value.isFinished) submitQuiz()
    }
}
```

**Pause / Reprise :**
```kotlin
fun pauseQuiz() {
    timerJob?.cancel()                    // arrêt immédiat du timer
    viewModelScope.launch {
        pausedQuizRepository.save(...)    // sérialisation Gson → Room
    }
}

fun resumeQuiz() {
    viewModelScope.launch {
        val saved = pausedQuizRepository.load() ?: return@launch
        pausedQuizRepository.clear()
        _uiState.value = QuizUiState(restored state)
        runTimer()                        // reprise du timer depuis remainingSeconds sauvegardé
    }
}
```

`startQuiz()` appelle `pausedQuizRepository.clear()` avant de tirer de nouvelles questions.

**Mélange des options :** au chargement, `QuizViewModel` mélange aléatoirement les 4 options de chaque question (via `withShuffledOptions()`) et met à jour `correctAnswer` en conséquence, pour que la bonne réponse ne soit jamais toujours à la même position.

**Logique de scoring :**
```kotlin
fun submitQuiz() {
    val score = questions.count { q -> answers[q.id] == q.correctAnswer }
    _uiState.update { it.copy(isFinished = true, score = score, passed = score >= 32) }
}
```

---

## Navigation (écrans)

| Route | Écran | Contenu |
|---|---|---|
| `home` | Accueil | Titre, règles résumées, boutons "Commencer" / "Reprendre" (conditionnel), "S'entraîner par thème", "Historique", "Paramètres", icône "Aide" |
| `quiz` | Examen | Question N/40, 4 options, chrono MM:SS, bouton Pause, BackHandler, barre de progression, son |
| `result` | Résultat | Score X/40, temps utilisé, mention Réussi/Échoué, détail, export |
| `history` | Historique | Liste des résultats passés, export individuel, vider l'historique |
| `settings` | Paramètres | Thème (Système/Clair/Sombre), toggle son, réinitialiser la progression d'entraînement, réinitialiser le cycle de l'examen |
| `help` | Aide | Guide utilisateur, règles de l'examen, thèmes, fonctionnalités, 7 liens officiels cliquables |
| `training_themes` | Entraînement (thèmes) | Liste des 5 thèmes + barre d'avancement `X/total`, retour Accueil |
| `training` | Entraînement (question) | Question d'un thème, feedback immédiat (vert/rouge), explication + lien source, "Suivant"/"Terminer" |

**Règle UX importante :** sur l'écran quiz, **aucun feedback immédiat** sur la bonne/mauvaise réponse (c'est un examen, pas un entraînement). Le feedback n'est affiché qu'à l'écran résultat.

---

## Pré-peuplement de la base de données

La BDD est peuplée **au premier lancement** depuis `questions.json`, directement dans
`QuestionRepository.drawStratifiedQuestions()` :

```kotlin
// QuestionRepository.kt
suspend fun drawStratifiedQuestions(): List<Question> {
    if (dao.count() == 0) {                        // premier lancement uniquement
        val json = context.resources.openRawResource(R.raw.questions)
            .bufferedReader().readText()
        val type = object : TypeToken<List<Question>>() {}.type
        val questions: List<Question> = Gson().fromJson(json, type)
        dao.insertAll(questions)
    }
    // tirage stratifié après seed garanti
    ...
}
```

> Seed et tirage sont **séquentiels** dans la même coroutine `suspend` — aucune race condition
> possible. L'ancienne approche (`RoomDatabase.Callback` async) causait un écran noir au premier
> lancement car la requête s'exécutait avant la fin du seed.

---

## Dépendances clés (`libs.versions.toml`)

```toml
[versions]
kotlin            = "2.0.21"
agp               = "8.13.2"
compose-bom       = "2024.12.01"
room              = "2.6.1"
hilt              = "2.52"
navigation        = "2.8.5"
coroutines        = "1.9.0"
gson              = "2.10.1"
datastore         = "1.1.1"

[libraries]
compose-bom            = { group = "androidx.compose", name = "compose-bom",                    version.ref = "compose-bom" }
compose-ui             = { group = "androidx.compose.ui", name = "ui" }
compose-material3      = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling     = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling-debug = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose       = { group = "androidx.activity", name = "activity-compose",               version = "1.9.3" }
appcompat              = { group = "androidx.appcompat", name = "appcompat",                     version = "1.7.0" }
room-runtime           = { group = "androidx.room", name = "room-runtime",                       version.ref = "room" }
room-ktx               = { group = "androidx.room", name = "room-ktx",                          version.ref = "room" }
room-compiler          = { group = "androidx.room", name = "room-compiler",                      version.ref = "room" }
hilt-android           = { group = "com.google.dagger", name = "hilt-android",                   version.ref = "hilt" }
hilt-compiler          = { group = "com.google.dagger", name = "hilt-android-compiler",          version.ref = "hilt" }
hilt-navigation        = { group = "androidx.hilt", name = "hilt-navigation-compose",            version = "1.2.0" }
navigation-compose     = { group = "androidx.navigation", name = "navigation-compose",            version.ref = "navigation" }
lifecycle-compose      = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose",      version = "2.8.7" }
coroutines-android     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
gson                   = { group = "com.google.code.gson", name = "gson",                        version.ref = "gson" }
datastore-preferences  = { group = "androidx.datastore", name = "datastore-preferences",          version.ref = "datastore" }

[plugins]
android-application    = { id = "com.android.application",             version.ref = "agp" }
kotlin-android         = { id = "org.jetbrains.kotlin.android",        version.ref = "kotlin" }
kotlin-compose         = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt                   = { id = "com.google.dagger.hilt.android",      version.ref = "hilt" }
ksp                    = { id = "com.google.devtools.ksp",              version = "2.0.21-1.0.27" }
```

---

## Règles de développement

1. **Kotlin uniquement** — pas de Java.
2. **Compose uniquement** pour l'UI — pas de XML layouts.
3. **StateFlow** pour l'état du ViewModel, pas LiveData.
4. **Coroutines** pour toutes les opérations I/O (Room, lecture JSON).
5. **Hilt** pour toutes les injections — pas de singletons manuels.
6. **`suspend` functions** dans les DAOs et Repositories ; le ViewModel utilise `viewModelScope`.
7. Les Composables sont **stateless** : ils reçoivent l'état et des lambdas d'événements.
8. **Pas de feedback immédiat** sur les réponses pendant l'examen — seulement à l'écran résultat.
9. Le timer tourne en continu même si on change de question ; il est géré dans le ViewModel.
10. Pas de permissions Android nécessaires (app 100 % offline).

---

## Flux utilisateur

```
[HomeScreen]
  ├─ "Commencer l'examen" ──► (si pause active → AlertDialog confirmation)
  │                               └─ confirmé → startQuiz() + [QuizScreen]
  ├─ "Reprendre l'examen" ──► resumeQuiz() + [QuizScreen]  (bouton visible si pause sauvegardée)
  │
  │   [QuizScreen]  ← timer 45 min démarre (ou reprend depuis remainingSeconds)
  │       ├─ Question N/40 (sans feedback immédiat)
  │       ├─ Chrono MM:SS (rouge < 5 min)
  │       ├─ Son au clic (si activé dans les paramètres)
  │       ├─ "Suivant" → question N+1
  │       ├─ Dernière question → bouton "Terminer"
  │       ├─ Timer à 00:00 → soumission automatique
  │       └─ "Pause" / retour système → pauseQuiz() → état sauvegardé Room → [HomeScreen]
  │                                                                   ↑
  │                                                   bouton "Reprendre" apparaît ──────┘
  │
  │       [ResultScreen]  (après Terminer ou expiration du timer)
  │           ├─ Score X/40 + temps utilisé
  │           ├─ RÉUSSI (≥ 32) ou ÉCHOUÉ (< 32)
  │           ├─ Détail : question par question
  │           ├─ "Exporter les résultats" → Intent.ACTION_SEND
  │           └─ "Recommencer" → [HomeScreen]
  ├─ "Historique" ──► [HistoryScreen]
  │                     ├─ Liste des résultats (date, score, durée, mention)
  │                     ├─ Icône partage sur chaque résultat → Intent.ACTION_SEND
  │                     └─ "Vider l'historique" (avec confirmation)
  ├─ "Paramètres" ──► [SettingsScreen]
  │                     ├─ Thème : Système / Clair / Sombre (persisté DataStore)
  │                     ├─ Son de sélection : activé/désactivé (persisté DataStore)
  │                     ├─ "Réinitialiser la progression" → TrainingProgressDao.clear()
  │                     └─ "Réinitialiser le cycle de l'examen" → ExamCycleDao.clear()
  └─ Icône Aide (Info) ──► [HelpScreen]
                            ├─ Règles de l'examen, thèmes, fonctionnalités
                            └─ 7 liens officiels cliquables (gouvernement, Légifrance, etc.)
```

---

## Commandes utiles

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

> **Contrainte environnement Claude Code web (sessions distantes) :** Gradle ne peut pas résoudre les dépendances Android (AGP, dépôts Google/Maven inaccessibles). La compilation locale est donc impossible dans ces sessions. Workflow à adopter : **revue du code uniquement** (lecture des fichiers, vérification des imports, logique, cohérence avec l'architecture) puis push sur la branche de travail — la CI GitHub Actions (`build.yml`) se charge de la compilation et de la validation.

---

## CI / GitHub Actions

Le workflow `build.yml` se déclenche **automatiquement** sur tout push vers `main` et sur les pull requests.
**Ne pas déclencher manuellement** (`workflow_dispatch`) après un push — le build est déjà en cours.

Le workflow `release.yml` se déclenche soit sur un tag `v.N.N.N`, soit manuellement via `workflow_dispatch` (input `version`) :
```bash
# Via tag (si accès push non bloqué)
git tag v.1.0.0
git push origin v.1.0.0

# Via workflow_dispatch (GitHub Actions → Release APK → Run workflow)
# Saisir le numéro de version dans l'input "Version tag"
```

La release publie l'APK (signé si secrets configurés, sinon debug) + `QUESTIONS.md` généré automatiquement.
