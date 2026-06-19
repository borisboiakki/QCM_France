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
| 5 | Vivre dans la société française | 44 | ~7 |
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
│   │   │   │   │   ├── QuestionDao.kt          # @Dao avec requêtes par thème
│   │   │   │   │   └── Converters.kt           # TypeConverters (List<String>)
│   │   │   │   ├── model/
│   │   │   │   │   └── Question.kt             # @Entity Room
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
│   │           └── questions.json              # Seed data (258 questions)
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
    @PrimaryKey val id: Int,
    val theme: String,           // L'un des 5 thèmes officiels
    val text: String,            // Énoncé de la question
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,   // "A", "B", "C" ou "D"
    val correctAnswers: String,  // JSON array — toutes les réponses officiellement acceptées
    val explanation: String = ""
)
```

### Format JSON de seed (`res/raw/questions.json`)

```json
[
  {
    "id": 1,
    "theme": "Principes et valeurs de la République",
    "text": "Complétez les paroles de la Marseillaise...",
    "optionA": "Le jour de gloire est arrivé !",
    "optionB": "Vive la France et la liberté !",
    "optionC": "Aux armes, citoyens !",
    "optionD": "Contre nous, la tyrannie !",
    "correctAnswer": "A",
    "correctAnswers": "[\"Le jour de gloire est arrivé\"]",
    "explanation": ""
  }
]
```

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
    val timerExpired: Boolean = false
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
| `home` | Accueil | Titre, règles résumées, bouton "Commencer l'examen" |
| `quiz` | Examen | Question N/40, 4 options, chrono MM:SS, barre de progression |
| `result` | Résultat | Score X/40, mention Réussi/Échoué (seuil 32), détail par question |

**Règle UX importante :** sur l'écran quiz, **aucun feedback immédiat** sur la bonne/mauvaise réponse (c'est un examen, pas un entraînement). Le feedback n'est affiché qu'à l'écran résultat.

---

## Pré-peuplement de la base de données

La BDD est peuplée **au premier lancement** depuis `questions.json` :

```kotlin
addCallback(object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val json = context.resources.openRawResource(R.raw.questions)
                .bufferedReader().readText()
            val questions = Gson().fromJson(json, Array<Question>::class.java).toList()
            dao.insertAll(questions)
        }
    }
})
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
gson              = "2.10.1"

[libraries]
compose-bom            = { group = "androidx.compose", name = "compose-bom",                   version.ref = "compose-bom" }
compose-ui             = { group = "androidx.compose.ui", name = "ui" }
compose-material3      = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling     = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
activity-compose       = { group = "androidx.activity", name = "activity-compose",              version = "1.9.3" }
room-runtime           = { group = "androidx.room", name = "room-runtime",                      version.ref = "room" }
room-ktx               = { group = "androidx.room", name = "room-ktx",                         version.ref = "room" }
room-compiler          = { group = "androidx.room", name = "room-compiler",                     version.ref = "room" }
hilt-android           = { group = "com.google.dagger", name = "hilt-android",                  version.ref = "hilt" }
hilt-compiler          = { group = "com.google.dagger", name = "hilt-android-compiler",         version.ref = "hilt" }
hilt-navigation        = { group = "androidx.hilt", name = "hilt-navigation-compose",           version = "1.2.0" }
navigation-compose     = { group = "androidx.navigation", name = "navigation-compose",           version.ref = "navigation" }
coroutines-android     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
gson                   = { group = "com.google.code.gson", name = "gson",                       version.ref = "gson" }

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
8. **Pas de feedback immédiat** sur les réponses pendant l'examen — seulement à l'écran résultat.
9. Le timer tourne en continu même si on change de question ; il est géré dans le ViewModel.
10. Pas de permissions Android nécessaires (app 100 % offline).

---

## Flux utilisateur

```
[HomeScreen]
  └─ "Commencer l'examen" ──► [QuizScreen]  ← timer 45 min démarre
                                  ├─ Question N/40 (sans feedback immédiat)
                                  ├─ Chrono MM:SS (rouge < 5 min)
                                  ├─ "Suivant" → question N+1
                                  ├─ Dernière question → bouton "Terminer"
                                  └─ Timer à 00:00 → soumission automatique
                                        │
                                        ▼
                                  [ResultScreen]
                                    ├─ Score X/40
                                    ├─ ✅ RÉUSSI (≥ 32) ou ❌ ÉCHOUÉ (< 32)
                                    ├─ Détail : question par question
                                    │   (bonne réponse en vert, mauvaise en rouge)
                                    └─ "Recommencer" → [HomeScreen]
```

---

## Commandes utiles

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```
