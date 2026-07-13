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

Chaque examen tire **28 questions de connaissances + 12 questions de mise en situation = 40**,
conformément à l'examen officiel. Le total par thème reste le même qu'avant l'introduction des
mises en situation (6/9/6/13/6) ; seule sa composition interne change. Le thème « Histoire,
géographie et culture » ne comporte aucune mise en situation (aucun scénario adapté à ce thème) :
il reste 100 % connaissances.

| # | Thème officiel | Connaissances (base) | Mise en situation (base) | Tirage / examen (connaissances + situation) |
|---|---|---|---|---|
| 1 | Principes et valeurs de la République | 39 | 15 | 3 + 3 = 6 |
| 2 | Système institutionnel et politique | 55 | 15 | 6 + 3 = 9 |
| 3 | Droits et devoirs | 37 | 15 | 3 + 3 = 6 |
| 4 | Histoire, géographie et culture | 83 | 0 | 13 + 0 = 13 |
| 5 | Vivre dans la société française | 44 | 15 | 3 + 3 = 6 |
| | **Total** | **258** | **60** | **28 + 12 = 40** |

**Stratégie de tirage :** tirage proportionnel par thème (stratified sampling) pour garantir que chaque thème est représenté, séparément pour le pool « connaissances » et le pool « mise en situation », puis ajustement pour atteindre exactement 28 et 12. L'ordre final des 40 questions est ensuite mélangé.

### Calcul du tirage (implémentation)

```kotlin
val TOTAL_QUESTIONS = 40   // 28 connaissances + 12 mise en situation
val connaissanceCounts = mapOf(
    "Principes et valeurs de la République" to 3,
    "Système institutionnel et politique"   to 6,
    "Droits et devoirs"                     to 3,
    "Histoire, géographie et culture"       to 13,
    "Vivre dans la société française"       to 3,
)
val situationCounts = mapOf(
    "Principes et valeurs de la République" to 3,
    "Système institutionnel et politique"   to 3,
    "Droits et devoirs"                     to 3,
    "Histoire, géographie et culture"       to 0,
    "Vivre dans la société française"       to 3,
)
// connaissanceCounts.sum() = 28, situationCounts.sum() = 12, total = 40.
```

**Anti-répétition entre examens :** le tirage n'utilise pas `ORDER BY RANDOM()` à chaque appel (ce qui permettrait à un thème de retirer les mêmes questions d'un examen à l'autre). À la place, chaque **couple thème + type** (connaissances ou mise en situation) a une permutation persistée de ses ids (table `exam_cycle`, clé `theme` pour les connaissances, `"$theme::situation"` pour les mises en situation — une simple chaîne libre, pas de migration de schéma nécessaire) et un curseur ; chaque examen consomme la suite de la permutation. Toutes les questions d'un thème/type sont donc utilisées une fois avant qu'une répétition ne survienne ; quand un cycle boucle, une nouvelle permutation est générée pour le tour suivant. Voir `QuestionRepository.drawIdsFromCycle()` et la section « Cycle de tirage de l'examen » plus bas.

### Chronomètre

- Durée : 45 minutes = 2 700 secondes (`ExamConstants.EXAM_DURATION_SECONDS`)
- Décompte basé sur une **échéance `SystemClock.elapsedRealtime()`** (pas un cumul de `delay(1000)`) : aucune dérive sur 45 min, le temps continue de s'écouler en arrière-plan
- Le timer tourne dans le ViewModel avec `viewModelScope`
- Affichage `MM:SS` en rouge (couleur `error` du thème) dans les 5 dernières minutes (`TIMER_WARNING_SECONDS`)
- À 00:00 : soumission automatique des réponses données, les non-répondues comptent comme fausses
- L'écran d'examen sauvegarde l'état à chaque `ON_STOP` (`QuizViewModel.saveSnapshot()`) : un examen en cours survit à une mort du processus

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
applicationId = "com.borisboiakki.qcmfrance"   // identifiant publié (le namespace du code reste com.example.qcmfrance)
compileSdk = 35
minSdk     = 26          // Android 8 — couvre >95 % des devices actifs
targetSdk  = 35
```

Kotlin : `2.0.21` | AGP : `8.13.2` | KSP : `2.0.21-1.0.27` | Gradle : `8.13`

Build **release** : R8 activé (`isMinifyEnabled = true`) + `shrinkResources` ; règles Gson dans
`proguard-rules.pro` (signatures génériques TypeToken + champs des modèles sérialisés).

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
│   └── generate_questions_md.py                 Génère QUESTIONS.md depuis questions.json + situational_questions.json
├── LICENSE                                      Licence MIT
├── QUESTIONS.md                                 Liste des 318 questions : 258 connaissances + 60 mise en situation (généré par release)
├── AUDIO_CREDITS.md                             Sources/licences audio + remplacement des placeholders
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/qcmfrance/
│   │   │   ├── data/
│   │   │   │   ├── ExamConstants.kt             Constantes officielles : durée 2700 s, seuil 32, alerte chrono 300 s
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt           Room @Database v8 (exportSchema) + migrations 1→2→3→4→5→6→7→8
│   │   │   │   │   ├── QuestionDao.kt           @Dao : getAllByTheme, getIdsByTheme(theme, isSituation), getByIds, countByTheme, insertAll, count
│   │   │   │   │   ├── QuizResultDao.kt         @Dao : getAll (Flow), insert, deleteAll
│   │   │   │   │   ├── PausedQuizDao.kt         @Dao : save (REPLACE), get, observe (Flow), delete
│   │   │   │   │   ├── TrainingProgressDao.kt   @Dao : save (REPLACE), get, observeAll (Flow), clear
│   │   │   │   │   └── ExamCycleDao.kt          @Dao : save (REPLACE), get, clear
│   │   │   │   ├── model/
│   │   │   │   │   ├── Question.kt              @Entity Room
│   │   │   │   │   ├── QuizResult.kt            @Entity Room : id, date, score, passed, duration
│   │   │   │   │   ├── PausedQuiz.kt            @Entity Room : singleton (PK=1), état sérialisé JSON
│   │   │   │   │   ├── TrainingProgress.kt      @Entity Room : PK=theme, currentIndex (point de reprise)
│   │   │   │   │   └── ExamCycle.kt             @Entity Room : PK=theme, permutation d'ids (JSON) + curseur
│   │   │   │   └── repository/
│   │   │   │       ├── QuestionRepository.kt    seedIfNeeded (2 fichiers JSON) + tirage stratifié 28 connaissances + 12 mise en situation, cyclé (exam_cycle), themes
│   │   │   │       ├── HistoryRepository.kt     sauvegarde et récupération de l'historique
│   │   │   │       ├── SettingsRepository.kt    DataStore : ThemeMode + soundEnabled + TextSizeMode
│   │   │   │       ├── PausedQuizRepository.kt  save/load/clear + PausedQuizState (Gson)
│   │   │   │       └── TrainingRepository.kt    questions par thème (ordre stable), avancement par thème
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                 Hilt @Module (AppDatabase, DAOs)
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt              9 routes : home/quiz/result/history/settings/help/training_themes/training/about
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt            titre, règles, Reprendre (si pause), S'entraîner par thème, AlertDialog, icône Aide
│   │   │   │   │   ├── QuizScreen.kt            question N/40, options, timer, bouton Pause, BackHandler, son
│   │   │   │   │   ├── ResultScreen.kt          score, RÉUSSI/ÉCHOUÉ, temps, filtre erreurs (FilterChip), détail, export, musique de fin (MediaPlayer)
│   │   │   │   │   ├── HistoryScreen.kt         liste des résultats, export par résultat, vider
│   │   │   │   │   ├── SettingsScreen.kt        thème (Système/Clair/Sombre), taille du texte (slider), toggle son, réinitialiser l'entraînement, réinitialiser le cycle de l'examen, À propos (défilable)
│   │   │   │   │   ├── HelpScreen.kt            guide utilisateur + 7 liens officiels cliquables
│   │   │   │   │   ├── AboutScreen.kt           version installée (PackageManager) + bouton vers les releases GitHub (téléchargement APK, sans permission)
│   │   │   │   │   ├── TrainingThemesScreen.kt  sélection du thème + barre d'avancement X/total par thème
│   │   │   │   │   └── TrainingScreen.kt        question du thème, feedback immédiat (vert/rouge), explication + lien source
│   │   │   │   ├── utils/
│   │   │   │   │   └── ResultExporter.kt        partage texte via Intent.ACTION_SEND
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── QuizViewModel.kt         QuizUiState, timer à échéance (cancellable), pauseQuiz/resumeQuiz/saveSnapshot, scoring, resetExamCycle
│   │   │   │   │   ├── TrainingViewModel.kt     TrainingUiState, themeProgress, startTheme/selectAnswer/confirmAnswer/next/restart/reset
│   │   │   │   │   ├── QuestionExt.kt           helper partagé withShuffledOptions() (examen + entraînement)
│   │   │   │   │   ├── HomeViewModel.kt         hasPausedQuiz : StateFlow<Boolean>
│   │   │   │   │   ├── HistoryViewModel.kt      Flow<List<QuizResult>>, clearHistory()
│   │   │   │   │   └── SettingsViewModel.kt     themeMode + soundEnabled + textSizeMode StateFlow
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt                 Material 3 dynamique, accepte ThemeMode + TextSizeMode (échelle typo)
│   │   │   │       ├── Color.kt                 palette + SuccessGreen/FailureRed partagées
│   │   │   │       └── Type.kt
│   │   │   ├── QcmFranceApplication.kt          @HiltAndroidApp
│   │   │   └── MainActivity.kt                  @AndroidEntryPoint, collecte ThemeMode + TextSizeMode
│   │   └── res/
│   │       ├── mipmap-*/                        Icônes adaptatives (fond bleu tricolore)
│   │       ├── raw/
│   │       │   ├── questions.json               258 questions de connaissances (seed)
│   │       │   ├── situational_questions.json   60 questions de mise en situation (seed), isSituation: true
│   │       │   ├── marseillaise.ogg             musique si examen réussi (domaine public — voir AUDIO_CREDITS.md)
│   │       │   └── marche_funebre.ogg           musique si examen échoué (domaine public — voir AUDIO_CREDITS.md)
│   │       ├── values/
│   │       │   ├── strings.xml                  Toutes les chaînes UI (~140) — aucun texte codé en dur dans les composables
│   │       │   ├── themes.xml                   Thème plateforme Material (clair)
│   │       │   └── colors.xml
│   │       ├── values-night/
│   │       │   └── themes.xml                   Variante sombre du thème plateforme
│   │       └── xml/
│   │           ├── backup_rules.xml             fullBackupContent (Android ≤ 11)
│   │           └── data_extraction_rules.xml    dataExtractionRules (Android 12+)
│   ├── schemas/                                 Schémas Room exportés (générés au build, à versionner)
│   ├── build.gradle.kts                         R8 + shrinkResources en release, room.schemaLocation
│   └── proguard-rules.pro                       Règles Gson (TypeToken, champs des modèles)
├── build.gradle.kts
├── gradle.properties                            android.useAndroidX=true, android.nonTransitiveRClass=true, org.gradle.jvmargs=-Xmx4g
├── settings.gradle.kts
├── gradlew / gradlew.bat                        Gradle wrapper 8.13
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
    val explanation: String = "",
    val source: String = "",        // URL de la source officielle (lien cliquable)
    val isSituation: Boolean = false // true = question de mise en situation (situational_questions.json)
)
```

> Le champ `correctAnswers` du JSON n'est **pas** chargé par l'app (le scoring n'utilise que
> `correctAnswer`) : la colonne a été supprimée en v7 (`MIGRATION_6_7`, recréation de table)
> et Gson ignore la clé lors du seed.
>
> `isSituation` (colonne ajoutée en v8, `MIGRATION_7_8`) distingue les questions de connaissances
> (`questions.json`, valeur par défaut `false`) des questions de mise en situation
> (`situational_questions.json`, `isSituation: true`). Les deux fichiers sont chargés dans la
> même table `questions` par `QuestionRepository.seedIfNeeded()`.

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
    @PrimaryKey val theme: String,   // clé de cycle : un thème officiel, ou "<thème>::situation"
    val orderJson: String,           // Gson List<Int> — permutation des ids du thème/type
    val cursor: Int                  // index de la prochaine question à tirer dans la permutation
)
```

Cycle de tirage de l'**examen** (pas l'entraînement). Voir « Cycle de tirage de l'examen » ci-dessous pour la logique complète. La table est créée par `MIGRATION_5_6` (BDD passée en v6). Réinitialisation via `QuestionRepository.resetExamCycle()` → `ExamCycleDao.clear()` (bouton « Réinitialiser le cycle de l'examen » dans les Paramètres).
>
> Le champ `theme` (PK) est une clé de cycle libre, pas nécessairement le nom exact du thème :
> pour séparer le cycle des connaissances de celui des mises en situation sur un même thème sans
> migrer le schéma de cette table, les mises en situation utilisent la clé `"<thème>::situation"`
> (cf. `QuestionRepository.drawIdsFromCycle()`).

### Cycle de tirage de l'examen

`QuestionRepository.drawIdsFromCycle(theme, count, isSituation)` remplace l'ancien tirage `ORDER BY RANDOM()` :

1. Calcule la clé de cycle (`theme` pour les connaissances, `"$theme::situation"` pour les mises en situation) et charge la permutation persistée (`exam_cycle.orderJson`) et son curseur pour cette clé ; si absente, ou si l'ensemble des ids ne correspond plus (questions ajoutées/supprimées), génère une nouvelle permutation aléatoire et repart du curseur 0.
2. Prend les `count` ids suivants à partir du curseur, en avançant le curseur.
3. Si la permutation est épuisée avant d'avoir pris `count` ids (fin d'un tour), génère une nouvelle permutation de l'ensemble des ids du thème/type pour le tour suivant — en plaçant les ids déjà pris dans ce tirage en fin de liste, pour ne pas les retirer immédiatement dans le même examen.
4. Persiste la permutation (éventuellement renouvelée) et le nouveau curseur, sous la clé de cycle.

`drawStratifiedQuestions()` appelle cette fonction une fois par thème pour `connaissanceCounts` (28 au total, `isSituation = false`) puis une fois par thème pour `situationCounts` (12 au total, `isSituation = true`), avant de concaténer et mélanger les 40 ids obtenus.

Résultat : toutes les questions d'un thème/type sont utilisées une fois avant qu'une répétition ne survienne d'un examen à l'autre. Indépendant du flux pause/reprise (`PausedQuiz`) : le cycle n'avance qu'au lancement d'un nouvel examen (`QuizViewModel.startQuiz()` → `drawStratifiedQuestions()`), jamais pendant une reprise.

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
- **Mises en situation incluses** : `TrainingRepository.questionsForTheme()`/`totalForTheme()` s'appuient sur `QuestionDao.getAllByTheme()`/`countByTheme()`, qui ne filtrent pas sur `isSituation` : les questions de mise en situation d'un thème apparaissent donc naturellement dans son entraînement, sans code de filtrage dédié.
- **Option shuffling** : réutilise `withShuffledOptions()` (déplacé dans `ui/viewmodel/QuestionExt.kt`, partagé par les deux ViewModels).
- **Réinitialisation** : Paramètres → « Réinitialiser la progression » (AlertDialog de confirmation) → `TrainingViewModel.resetTraining()` → `TrainingRepository.resetAll()`.

---

### Format JSON de seed (`res/raw/questions.json`, `res/raw/situational_questions.json`)

Même schéma dans les deux fichiers ; seul `situational_questions.json` renseigne
`"isSituation": true` sur chaque entrée (absent de `questions.json`, donc `false` par défaut).

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

```json
[
  {
    "id": 1001,
    "theme": "Principes et valeurs de la République",
    "text": "Un parent d'élève demande que son enfant soit dispensé du cours de sciences de la vie pour un motif religieux. Que répond l'école publique ?",
    "optionA": "Elle accorde la dispense sans discussion",
    "optionB": "Elle rappelle que les programmes scolaires nationaux s'appliquent à tous les élèves, sans dispense pour motif religieux",
    "optionC": "Elle exclut l'enfant de l'école",
    "optionD": "Elle organise un cours séparé selon la religion de chaque élève",
    "correctAnswer": "B",
    "explanation": "L'école publique est laïque : les programmes nationaux s'imposent à tous les élèves. Une dispense n'est possible que pour des motifs médicaux, jamais religieux.",
    "source": "https://www.education.gouv.fr/",
    "isSituation": true
  }
]
```

> `correctAnswers` reste présent dans `questions.json` (donnée de référence) mais n'est **pas**
> chargé par l'app : le champ n'existe plus dans l'entité Room et Gson ignore les clés inconnues.
> Les ids de `situational_questions.json` démarrent à **1001** pour ne jamais entrer en collision
> avec ceux de `questions.json` (1 à 258, avec de la marge pour de futurs ajouts).

---

## DAO

```kotlin
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY id")
    suspend fun getAllByTheme(theme: String): List<Question>

    @Query("SELECT id FROM questions WHERE theme = :theme AND isSituation = :isSituation ORDER BY id")
    suspend fun getIdsByTheme(theme: String, isSituation: Boolean): List<Int>

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
    val passed: Boolean = false,                   // score >= ExamConstants.PASS_THRESHOLD
    // Timer
    val remainingSeconds: Int = ExamConstants.EXAM_DURATION_SECONDS,
    val timerExpired: Boolean = false,
    val isLoading: Boolean = true                  // spinner pendant le chargement des questions
)
```

**Événements :** `SelectAnswer(letter)`, `NextQuestion`, `SubmitQuiz`, `RestartQuiz`, `PauseQuiz`, `ResumeQuiz`, `SaveSnapshot` (auto-sauvegarde `ON_STOP`)

**Logique du timer** (échéance `elapsedRealtime`, pas de dérive) :
```kotlin
private var timerJob: Job? = null   // stocké pour permettre l'annulation (pause)

private fun runTimer() {
    timerJob?.cancel()
    val deadline = SystemClock.elapsedRealtime() + _uiState.value.remainingSeconds * 1000L
    timerJob = viewModelScope.launch {
        while (!_uiState.value.isFinished) {
            val remaining = ((deadline - SystemClock.elapsedRealtime() + 999) / 1000)
                .coerceAtLeast(0).toInt()
            if (remaining != _uiState.value.remainingSeconds)
                _uiState.update { it.copy(remainingSeconds = remaining) }
            if (remaining == 0) break
            delay(250L)
        }
        if (_uiState.value.remainingSeconds == 0 && !_uiState.value.isFinished) {
            _uiState.update { it.copy(timerExpired = true) }
            submitQuiz()
        }
    }
}
```

**Pause / Reprise / Auto-sauvegarde :**
```kotlin
fun pauseQuiz() {
    timerJob?.cancel()          // arrêt immédiat du timer
    saveSnapshot()              // sérialisation Gson → Room
}

fun saveSnapshot() {            // appelé aussi à chaque ON_STOP de QuizScreen (timer non arrêté)
    // garde isLoading/isFinished/questions vides, puis pausedQuizRepository.save(...)
}

fun resumeQuiz() {
    timerJob?.cancel()
    _uiState.value = QuizUiState()          // reset synchrone (anti état périmé)
    viewModelScope.launch {
        val saved = pausedQuizRepository.load() ?: return@launch
        // la sauvegarde n'est PAS effacée ici : filet de sécurité jusqu'à la soumission
        _uiState.value = QuizUiState(restored state)
        runTimer()
    }
}
```

`startQuiz()` réinitialise l'état de façon **synchrone** (un `isFinished=true` résiduel déclencherait
la navigation immédiate vers l'ancien résultat), puis appelle `pausedQuizRepository.clear()` avant de
tirer de nouvelles questions. `submitQuiz()` est **idempotent** (garde `isFinished`) et efface la
sauvegarde de pause.

**Mélange des options :** au chargement, `QuizViewModel` mélange aléatoirement les 4 options de chaque question (via `withShuffledOptions()`) et met à jour `correctAnswer` en conséquence, pour que la bonne réponse ne soit jamais toujours à la même position.

**Logique de scoring :**
```kotlin
fun submitQuiz() {
    if (_uiState.value.isFinished) return   // idempotent (double tap / course avec le timer)
    val score = questions.count { q -> answers[q.id] == q.correctAnswer }
    _uiState.update { it.copy(isFinished = true, score = score, passed = score >= ExamConstants.PASS_THRESHOLD) }
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
| `settings` | Paramètres | Thème (Système/Clair/Sombre), toggle son, réinitialiser la progression d'entraînement, réinitialiser le cycle de l'examen, accès « À propos » |
| `help` | Aide | Guide utilisateur, règles de l'examen, thèmes, fonctionnalités, 7 liens officiels cliquables |
| `about` | À propos / Mises à jour | Version installée (lue via `PackageManager`, sans réseau) + bouton ouvrant `github.com/borisboiakki/qcm_france/releases/latest` dans le navigateur pour télécharger l'APK — aucune permission ajoutée |
| `training_themes` | Entraînement (thèmes) | Liste des 5 thèmes + barre d'avancement `X/total`, retour Accueil |
| `training` | Entraînement (question) | Question d'un thème, feedback immédiat (vert/rouge), explication + lien source, "Suivant"/"Terminer" |

**Règle UX importante :** sur l'écran quiz, **aucun feedback immédiat** sur la bonne/mauvaise réponse (c'est un examen, pas un entraînement). Le feedback n'est affiché qu'à l'écran résultat.

---

## Pré-peuplement de la base de données

La BDD est peuplée **au premier lancement** depuis `questions.json` **et**
`situational_questions.json`, directement dans `QuestionRepository.seedIfNeeded()` (appelée par
`drawStratifiedQuestions()` et par le chemin entraînement) :

```kotlin
// QuestionRepository.kt
suspend fun seedIfNeeded() {
    if (dao.count() == 0) {                         // premier lancement uniquement
        val type = object : TypeToken<List<Question>>() {}.type
        val connaissance: List<Question> = gson.fromJson(
            context.resources.openRawResource(R.raw.questions).bufferedReader().readText(), type
        )
        val situation: List<Question> = gson.fromJson(
            context.resources.openRawResource(R.raw.situational_questions).bufferedReader().readText(), type
        )
        dao.insertAll(connaissance + situation)
    }
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
compose-ui-tooling-debug = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose       = { group = "androidx.activity", name = "activity-compose",               version = "1.9.3" }
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
11. **Toutes les chaînes UI dans `res/values/strings.xml`** — aucun texte codé en dur dans les composables.
12. **Constantes de l'examen** (durée, seuil, alerte chrono) uniquement via `ExamConstants` — pas de littéraux 2700/32/300.
13. **Accessibilité** : lignes d'option via `Modifier.selectable(role = RadioButton)` + `RadioButton(onClick = null)` dans un `selectableGroup()` ; interrupteurs via `Modifier.toggleable(role = Switch)`.

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
  │                     ├─ Taille du texte : Petit / Moyen / Grand (slider, persisté DataStore)
  │                     ├─ Son de sélection : activé/désactivé (persisté DataStore)
  │                     ├─ "Réinitialiser la progression" → TrainingProgressDao.clear()
  │                     ├─ "Réinitialiser le cycle de l'examen" → ExamCycleDao.clear()
  │                     └─ "Mises à jour et à propos" ──► [AboutScreen]
  │                                                        ├─ Version installée (PackageManager)
  │                                                        └─ "Télécharger la dernière version sur GitHub" → navigateur (releases/latest)
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
