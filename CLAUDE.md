# QCM France — Android App

Application Android de type QCM (Questionnaire à Choix Multiples) : 40 questions tirées aléatoirement dans une base de plusieurs centaines de questions.

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

Kotlin : `2.0.x` | AGP : `8.x`

---

## Structure du projet

```
QCM_France/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/qcmfrance/
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt          # Room @Database
│   │   │   │   │   ├── QuestionDao.kt          # @Dao avec requête RANDOM
│   │   │   │   │   └── Converters.kt           # TypeConverters si besoin
│   │   │   │   ├── model/
│   │   │   │   │   └── Question.kt             # @Entity Room + data class
│   │   │   │   └── repository/
│   │   │   │       └── QuestionRepository.kt
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                # Hilt modules
│   │   │   ├── ui/
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── QuizScreen.kt
│   │   │   │   │   └── ResultScreen.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── QuizViewModel.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt
│   │   │   │       ├── Color.kt
│   │   │   │       └── Type.kt
│   │   │   └── MainActivity.kt
│   │   └── res/
│   │       └── raw/
│   │           └── questions.json              # Seed data (pré-peuplage BDD)
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── libs.versions.toml                      # Version catalog
```

---

## Modèle de données

### Entité Room — `Question`

```kotlin
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,                    // Énoncé
    val optionA: String,
    val optionB: String,
    val optionC: String?,                // Nullable : certaines questions n'ont que 2 options
    val optionD: String?,
    val correctAnswer: String,           // "A", "B", "C" ou "D"
    val explanation: String = "",        // Explication facultative affichée après réponse
    val category: String = "general",    // Ex. : "histoire", "géographie", "code_route"…
    val difficulty: Int = 1              // 1=facile, 2=moyen, 3=difficile
)
```

### Format JSON de seed (`res/raw/questions.json`)

```json
[
  {
    "text": "Quelle est la capitale de la France ?",
    "optionA": "Lyon",
    "optionB": "Marseille",
    "optionC": "Paris",
    "optionD": "Bordeaux",
    "correctAnswer": "C",
    "explanation": "Paris est la capitale depuis...",
    "category": "géographie",
    "difficulty": 1
  }
]
```

---

## DAO — requête centrale

```kotlin
@Dao
interface QuestionDao {
    // 40 questions aléatoires (toutes catégories)
    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomQuestions(count: Int = 40): List<Question>

    // Par catégorie
    @Query("SELECT * FROM questions WHERE category = :cat ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomByCategory(cat: String, count: Int = 40): List<Question>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int
}
```

---

## ViewModel — état du quiz

```kotlin
data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val answers: Map<Int, String> = emptyMap(),   // questionId → réponse choisie
    val isFinished: Boolean = false,
    val score: Int = 0
)

// Événements : NextQuestion, SelectAnswer, RestartQuiz
```

---

## Navigation (écrans)

| Route | Écran |
|---|---|
| `home` | Accueil : bouton "Commencer", catégorie optionnelle |
| `quiz` | Question courante + options + progression (1/40) |
| `result` | Score final + détail des réponses |

---

## Pré-peuplement de la base de données

La BDD est peuplée **au premier lancement** depuis `questions.json` via un `RoomDatabase.Callback` :

```kotlin
object AppDatabase {
    // Dans le Builder Room :
    addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            // Lire res/raw/questions.json et insérer via coroutine
        }
    })
}
```

---

## Dépendances clés (`libs.versions.toml`)

```toml
[versions]
kotlin            = "2.0.21"
agp               = "8.7.3"
compose-bom       = "2024.12.01"
room              = "2.6.1"
hilt              = "2.52"
navigation        = "2.8.5"
coroutines        = "1.9.0"

[libraries]
# Compose BOM
compose-bom            = { group = "androidx.compose", name = "compose-bom",          version.ref = "compose-bom" }
compose-ui             = { group = "androidx.compose.ui", name = "ui" }
compose-material3      = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling     = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
activity-compose       = { group = "androidx.activity", name = "activity-compose",     version = "1.9.3" }
# Room
room-runtime           = { group = "androidx.room", name = "room-runtime",             version.ref = "room" }
room-ktx               = { group = "androidx.room", name = "room-ktx",                version.ref = "room" }
room-compiler          = { group = "androidx.room", name = "room-compiler",            version.ref = "room" }
# Hilt
hilt-android           = { group = "com.google.dagger", name = "hilt-android",         version.ref = "hilt" }
hilt-compiler          = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation        = { group = "androidx.hilt", name = "hilt-navigation-compose",  version = "1.2.0" }
# Navigation
navigation-compose     = { group = "androidx.navigation", name = "navigation-compose",  version.ref = "navigation" }
# Coroutines
coroutines-android     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application    = { id = "com.android.application",              version.ref = "agp" }
kotlin-android         = { id = "org.jetbrains.kotlin.android",         version.ref = "kotlin" }
kotlin-compose         = { id = "org.jetbrains.kotlin.plugin.compose",  version.ref = "kotlin" }
hilt                   = { id = "com.google.dagger.hilt.android",       version.ref = "hilt" }
ksp                    = { id = "com.google.devtools.ksp",               version = "2.0.21-1.0.27" }
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
8. Le JSON de questions est en `res/raw/` et parsé avec `kotlinx.serialization` ou `Gson`.
9. Pas de permissions Android nécessaires (app 100 % offline).
10. Taille minimale du JSON : ≥ 200 questions pour que le tirage de 40 soit varié.

---

## Flux utilisateur

```
[HomeScreen]
  └─ "Démarrer" ──► [QuizScreen]
                        ├─ Affiche question N (1..40)
                        ├─ Sélection réponse → feedback visuel (bonne/mauvaise)
                        ├─ "Suivant" → question N+1
                        └─ Dernière question → [ResultScreen]
                                                  ├─ Score X/40
                                                  ├─ Détail par question
                                                  └─ "Recommencer" → [HomeScreen]
```

---

## Commandes utiles

```bash
# Build debug
./gradlew assembleDebug

# Tests unitaires
./gradlew test

# Tests instrumented (Room in-memory)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```
