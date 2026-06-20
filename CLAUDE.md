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

Requête DAO par thème :
```kotlin
@Query("SELECT * FROM questions WHERE theme = :theme ORDER BY RANDOM() LIMIT :count")
suspend fun getRandomByTheme(theme: String, count: Int): List<Question>
```

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
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/qcmfrance/
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt           Room @Database v2 + migrations
│   │   │   │   │   ├── Converters.kt            @TypeConverter List<String> ↔ JSON String
│   │   │   │   │   ├── QuestionDao.kt           @Dao : getRandomByTheme, insertAll, count
│   │   │   │   │   └── QuizResultDao.kt         @Dao : getAll (Flow), insert, deleteAll
│   │   │   │   ├── model/
│   │   │   │   │   ├── Question.kt              @Entity Room
│   │   │   │   │   └── QuizResult.kt            @Entity Room : id, date, score, passed, duration
│   │   │   │   └── repository/
│   │   │   │       ├── QuestionRepository.kt    seed + tirage stratifié 6-9-6-13-6
│   │   │   │       ├── HistoryRepository.kt     sauvegarde et récupération de l'historique
│   │   │   │       └── SettingsRepository.kt    DataStore : ThemeMode + soundEnabled
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                 Hilt @Module (AppDatabase, DAOs)
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt              5 routes : home/quiz/result/history/settings
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt            titre, règles, boutons historique/paramètres
│   │   │   │   │   ├── QuizScreen.kt            question N/40, options, timer, son conditionnel
│   │   │   │   │   ├── ResultScreen.kt          score, RÉUSSI/ÉCHOUÉ, temps, détail, export
│   │   │   │   │   ├── HistoryScreen.kt         liste des résultats, export par résultat, vider
│   │   │   │   │   └── SettingsScreen.kt        thème (Système/Clair/Sombre) + toggle son
│   │   │   │   ├── utils/
│   │   │   │   │   └── ResultExporter.kt        partage texte via Intent.ACTION_SEND
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── QuizViewModel.kt         QuizUiState, timer, scoring, tirage stratifié
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
│   │       │   └── questions.json               258 questions (seed)
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
    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomByTheme(theme: String, count: Int): List<Question>

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

**Événements :** `SelectAnswer(letter)`, `NextQuestion`, `SubmitQuiz`, `RestartQuiz`

**Logique du timer :**
```kotlin
// Dans QuizViewModel.init
viewModelScope.launch {
    while (_uiState.value.remainingSeconds > 0 && !_uiState.value.isFinished) {
        delay(1000L)
        _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
    }
    if (_uiState.value.remainingSeconds == 0) submitQuiz()
}
```

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
| `home` | Accueil | Titre, règles résumées, boutons "Commencer", "Historique", "Paramètres" |
| `quiz` | Examen | Question N/40, 4 options, chrono MM:SS, barre de progression, son conditionnel |
| `result` | Résultat | Score X/40, temps utilisé, mention Réussi/Échoué, détail, export |
| `history` | Historique | Liste des résultats passés, export individuel, vider l'historique |
| `settings` | Paramètres | Thème (Système/Clair/Sombre), toggle son de sélection |

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
  ├─ "Commencer l'examen" ──► [QuizScreen]  ← timer 45 min démarre
  │                               ├─ Question N/40 (sans feedback immédiat)
  │                               ├─ Chrono MM:SS (rouge < 5 min)
  │                               ├─ Son au clic (si activé dans les paramètres)
  │                               ├─ "Suivant" → question N+1
  │                               ├─ Dernière question → bouton "Terminer"
  │                               └─ Timer à 00:00 → soumission automatique
  │                                       │
  │                                       ▼
  │                               [ResultScreen]
  │                                 ├─ Score X/40 + temps utilisé
  │                                 ├─ RÉUSSI (≥ 32) ou ÉCHOUÉ (< 32)
  │                                 ├─ Détail : question par question
  │                                 ├─ "Exporter les résultats" → Intent.ACTION_SEND
  │                                 └─ "Recommencer" → [HomeScreen]
  ├─ "Historique" ──► [HistoryScreen]
  │                     ├─ Liste des résultats (date, score, durée, mention)
  │                     ├─ Icône partage sur chaque résultat → Intent.ACTION_SEND
  │                     └─ "Vider l'historique" (avec confirmation)
  └─ "Paramètres" ──► [SettingsScreen]
                        ├─ Thème : Système / Clair / Sombre (persisté DataStore)
                        └─ Son de sélection : activé/désactivé (persisté DataStore)
```

---

## Commandes utiles

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

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
