# QCM France — Android App

Application Android de préparation aux **examens civiques français** : naturalisation, carte de
résident (CR) et carte de séjour pluriannuelle (CSP).

---

## Les trois QCM couverts (`ExamMode`)

L'utilisateur choisit sur l'écran d'accueil l'examen qu'il prépare ; l'examen blanc, l'entraînement
et les succès s'y adaptent. Le choix est persisté (DataStore, clé `exam_mode`).

| Code | Examen | Fichier de connaissances | Plage d'ids | Connaissances | Tirables (avec mises en situation) |
|---|---|---|---|---|---|
| `NAT` | Naturalisation | `res/raw/questions.json` | 1 – 999 | 258 | 338 |
| `CR` | Carte de résident | `res/raw/questions_cr.json` | 2001 – 2999 | 209 | 289 |
| `CSP` | Carte de séjour pluriannuelle | `res/raw/questions_csp.json` | 3001 – 3999 | 191 | 271 |
| `ALL` | *(communes aux trois)* | `res/raw/situational_questions.json` | 1000 – 1999 | 80 mises en situation | — |

**738 questions au total** dans la base.

Les **mises en situation** (`isSituation = true`, `examMode = "ALL"`) et les **fiches thématiques
officielles** sont communes aux trois examens. Toute la mécanique d'examen est elle aussi
identique — seule la liste de questions de connaissances change.

> Le ministère ne publie que les **énoncés** : propositions, bonnes réponses, explications, sources
> et variantes sont rédigées pour l'application. Conventions de rédaction et historique des lots :
> **`docs/MULTI_QCM_PLAN.md`**. Contrôle avant tout commit de contenu :
> `python3 scripts/check_questions_consistency.py`.

---

## Règles officielles de l'examen

Identiques pour les trois QCM.

| Règle | Valeur |
|---|---|
| Nombre de questions | **40** tirées aléatoirement dans la base du QCM choisi |
| Seuil de réussite | **32/40 minimum (80 %)** |
| Durée maximale | **45 minutes** (chronomètre décompte affiché) |
| Format | Une seule bonne réponse parmi 4 propositions |
| Support | Numérique (cette app) |

### Les 5 thèmes officiels et leur répartition dans la base

Le tableau ci-dessous décrit le mode **naturalisation** ; la répartition du tirage (colonne de
droite) est **la même pour les trois QCM**, chaque liste officielle comptant au moins 13 questions
dans « Histoire, géographie et culture ».

Chaque examen tire **28 questions de connaissances + 12 questions de mise en situation = 40**,
conformément à l'examen officiel. Le total par thème reste le même qu'avant l'introduction des
mises en situation (6/9/6/13/6) ; seule sa composition interne change. Le thème « Histoire,
géographie et culture » ne comporte aucune mise en situation (aucun scénario adapté à ce thème) :
il reste 100 % connaissances.

| # | Thème officiel | Connaissances (base) | Mise en situation (base) | Tirage / examen (connaissances + situation) |
|---|---|---|---|---|
| 1 | Principes et valeurs de la République | 39 | 20 | 3 + 3 = 6 |
| 2 | Système institutionnel et politique | 55 | 20 | 6 + 3 = 9 |
| 3 | Droits et devoirs | 37 | 20 | 3 + 3 = 6 |
| 4 | Histoire, géographie et culture | 83 | 0 | 13 + 0 = 13 |
| 5 | Vivre dans la société française | 44 | 20 | 3 + 3 = 6 |
| | **Total** | **258** | **80** | **28 + 12 = 40** |

**Stratégie de tirage :** tirage proportionnel par thème (stratified sampling) pour garantir que chaque thème est représenté, séparément pour le pool « connaissances » et le pool « mise en situation », puis ajustement pour atteindre exactement 28 et 12. **Ordre de présentation :** comme dans l'examen officiel, les 28 questions de connaissances sont posées d'abord, puis les 12 mises en situation ; chaque bloc est mélangé séparément. Sur l'écran d'examen et dans le détail de l'écran résultat, les mises en situation portent un badge « Mise en situation » (`ui/components/SituationBadge.kt`).

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

**Anti-répétition entre examens :** le tirage n'utilise pas `ORDER BY RANDOM()` à chaque appel (ce qui permettrait à un thème de retirer les mêmes questions d'un examen à l'autre). À la place, chaque **triplet QCM + thème + type** (connaissances ou mise en situation) a une permutation persistée de ses ids (table `exam_cycle`, clé produite par `ExamMode.cycleKey()` : `theme` / `"$theme::situation"` pour la naturalisation, `"CR::$theme"` / `"CR::$theme::situation"` pour les autres QCM — une simple chaîne libre, pas de migration de schéma nécessaire) et un curseur ; chaque examen consomme la suite de la permutation. Toutes les questions d'un thème/type sont donc utilisées une fois avant qu'une répétition ne survienne ; quand un cycle boucle, une nouvelle permutation est générée pour le tour suivant. Voir `QuestionRepository.drawIdsFromCycle()` et la section « Cycle de tirage de l'examen » plus bas.

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
`proguard-rules.pro` (signatures génériques TypeToken + **keep complet des modèles Gson** —
R8 full mode traite une classe créée uniquement par réflexion, comme `QuestionVariant` ou
`Fiche`, comme jamais instanciée si on ne garde que ses champs).

---

## Structure du projet

```
QCM_France/
├── .github/
│   └── workflows/
│       ├── build.yml                            CI — build APK debug sur push/PR vers main
│       ├── release.yml                          Release — build + publication sur tag v.N.N.N
│       │                                        (supporte aussi workflow_dispatch avec input version)
│       └── update-fiches.yml                    Manuel (workflow_dispatch) — régénère res/raw/fiches.json + ouvre une PR
├── scripts/
│   ├── generate_questions_md.py                 Génère QUESTIONS.md (une section par QCM + mises en situation)
│   ├── check_questions_consistency.py           Contrôle des 4 fichiers de questions (ids, propositions, variantes, cohérence inter-QCM, couverture, indice de longueur des mises en situation)
│   └── fetch_fiches.py                          Scrape les fiches thématiques officielles → res/raw/fiches.json (offline)
├── LICENSE                                      Licence MIT
├── QUESTIONS.md                                 Liste des questions des 3 QCM + mises en situation (généré par release)
├── docs/
│   └── MULTI_QCM_PLAN.md                        Plan et suivi de l'extension multi-QCM (lots de rédaction CR/CSP)
├── AUDIO_CREDITS.md                             Sources/licences audio + remplacement des placeholders
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/qcmfrance/
│   │   │   ├── data/
│   │   │   │   ├── ExamConstants.kt             Constantes officielles : durée 2700 s, seuil 32, alerte chrono 300 s
│   │   │   │   ├── db/
│   │   │   │   │   ├── AppDatabase.kt           Room @Database v12 (exportSchema) + migrations 1→2→…→11→12 + @TypeConverters(Converters)
│   │   │   │   │   ├── Converters.kt            TypeConverter Room : List<QuestionVariant> ↔ JSON (colonne `variants`)
│   │   │   │   │   ├── QuestionDao.kt           @Dao : getAllByTheme(theme, mode), getIdsByTheme(theme, isSituation, mode), getByIds, insertAll, count, countForMode, countSeenForMode
│   │   │   │   │   ├── QuizResultDao.kt         @Dao : getAll (Flow), insert, deleteAll
│   │   │   │   │   ├── PausedQuizDao.kt         @Dao : save (REPLACE), get, observe (Flow), delete
│   │   │   │   │   ├── TrainingProgressDao.kt   @Dao : save (REPLACE), get, observeAll (Flow), clear
│   │   │   │   │   ├── ExamCycleDao.kt          @Dao : save (REPLACE), get, clear
│   │   │   │   │   ├── AchievementDao.kt        @Dao : observeAll (Flow), get, upsert (REPLACE), clear
│   │   │   │   │   ├── SeenQuestionDao.kt       @Dao : insertAll (IGNORE), count, clear
│   │   │   │   │   └── ReadFicheDao.kt          @Dao : insert (IGNORE), observeReadIds (Flow), count, clear
│   │   │   │   ├── model/
│   │   │   │   │   ├── ExamMode.kt              Enum des 3 QCM (NAT/CR/CSP) + clés de cycle et de progression
│   │   │   │   │   ├── Question.kt              @Entity Room (colonne examMode)
│   │   │   │   │   ├── QuizResult.kt            @Entity Room : id, date, score, passed, duration
│   │   │   │   │   ├── PausedQuiz.kt            @Entity Room : singleton (PK=1), état sérialisé JSON
│   │   │   │   │   ├── TrainingProgress.kt      @Entity Room : PK=theme, currentIndex (point de reprise)
│   │   │   │   │   ├── ExamCycle.kt             @Entity Room : PK=theme, permutation d'ids (JSON) + curseur
│   │   │   │   │   ├── Achievement.kt           Catalogue statique (Achievements.ALL) + AchievementRecord (@Entity) + AchievementState
│   │   │   │   │   ├── SeenQuestion.kt          @Entity Room : PK=questionId (questions déjà vues en examen)
│   │   │   │   │   ├── ReadFiche.kt             @Entity Room : PK=ficheId (fiches déjà consultées — avancement de lecture + succès fiches)
│   │   │   │   │   └── Fiche.kt                 Modèles Gson (FichesData/FicheTheme/Fiche) — fiches offline, PAS de Room
│   │   │   │   └── repository/
│   │   │   │       ├── QuestionRepository.kt    seedIfNeeded (4 fichiers JSON) + tirage stratifié par QCM : 28 connaissances puis 12 mise en situation (ordre garanti), cyclé (exam_cycle), themes
│   │   │   │       ├── HistoryRepository.kt     sauvegarde et récupération de l'historique
│   │   │   │       ├── SettingsRepository.kt    DataStore : ThemeMode + soundEnabled + TextSizeMode + ExamMode (QCM choisi)
│   │   │   │       ├── PausedQuizRepository.kt  save/load/clear + PausedQuizState (Gson)
│   │   │   │       ├── TrainingRepository.kt    questions par QCM + thème (ordre stable), avancement par QCM + thème
│   │   │   │       ├── AchievementRepository.kt moteur de déblocage (unlock/onExamCompleted/onThemeCompleted), newlyUnlocked (SharedFlow), observe
│   │   │   │       └── FichesRepository.kt      lit res/raw/fiches.json (Gson, cache mémoire) — themes/fichesForTheme/fiche + suivi de lecture (observeReadIds/markRead/readCount/totalFichesCount/clearRead via read_fiche)
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                 Hilt @Module (AppDatabase, DAOs)
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   │   └── NavGraph.kt              13 routes : home/quiz/result/history/settings/help/resources/training_themes/training/about/achievements/fiches_list/fiche_detail + overlay popup succès
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt            titre, sélecteur de QCM (NAT/CR/CSP), Reprendre (si pause), S'entraîner par thème, Succès, AlertDialog, icônes Ressources / Aide / Paramètres
│   │   │   │   │   ├── QuizScreen.kt            question N/40, badge « Mise en situation », options, timer, bouton Pause, BackHandler, son
│   │   │   │   │   ├── ResultScreen.kt          score, RÉUSSI/ÉCHOUÉ, temps, filtre erreurs (FilterChip), détail (badge mise en situation), export, musique de fin (MediaPlayer)
│   │   │   │   │   ├── HistoryScreen.kt         liste des résultats, export par résultat, vider
│   │   │   │   │   ├── SettingsScreen.kt        thème (Système/Clair/Sombre), taille du texte (slider), toggle son, réinitialiser l'entraînement, réinitialiser le cycle de l'examen, réinitialiser les succès, À propos (défilable)
│   │   │   │   │   ├── HelpScreen.kt            guide utilisateur (règles, thèmes, fonctionnalités) — les liens officiels sont dans ResourcesScreen
│   │   │   │   │   ├── ResourcesScreen.kt       ressources complémentaires : liens officiels cliquables en 3 sections (Textes officiels / Examen civique et tests / Fiches thématiques) — liens en ligne uniquement
│   │   │   │   │   ├── AboutScreen.kt           version installée (PackageManager) + bouton vers les releases GitHub (téléchargement APK, sans permission)
│   │   │   │   │   ├── TrainingThemesScreen.kt  écran « S'entraîner » : 3 sections de QCM × 5 cartes X/total + section « sources officielles » (5 cartes de fiches, avancement de lecture X/total)
│   │   │   │   │   ├── TrainingScreen.kt        question du thème, feedback immédiat (vert/rouge), explication + lien source
│   │   │   │   │   ├── AchievementsScreen.kt    liste des succès groupés par catégorie (Examen / Entraînement ×3 QCM / Fiches), verrouillés grisés, barres X/target
│   │   │   │   │   ├── FichesListScreen.kt      fiches offline : liste des fiches d'un thème (lu/non lu : carte atténuée + coche verte)
│   │   │   │   │   └── FicheDetailScreen.kt     fiches offline : rendu markdown + « Voir en ligne » + source
│   │   │   │   ├── components/
│   │   │   │   │   ├── AchievementUnlockedBanner.kt  bandeau animé « Succès débloqué ! » (overlay global, slide-in, auto-dismiss)
│   │   │   │   │   ├── MarkdownText.kt          rendu markdown minimal en Compose (titres/listes/gras/liens), sans dépendance
│   │   │   │   │   └── SituationBadge.kt        badge « Mise en situation » (examen + détail du résultat)
│   │   │   │   ├── utils/
│   │   │   │   │   └── ResultExporter.kt        partage texte via Intent.ACTION_SEND
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── QuizViewModel.kt         QuizUiState, timer à échéance (cancellable), pauseQuiz/resumeQuiz/saveSnapshot, scoring, resetExamCycle
│   │   │   │   │   ├── TrainingViewModel.kt     TrainingUiState, themeProgress, startTheme/selectAnswer/confirmAnswer/next/restart/reset
│   │   │   │   │   ├── QuestionExt.kt           helpers partagés allAnswerSets() + pickVariant() + withShuffledOptions() (examen + entraînement)
│   │   │   │   │   ├── HomeViewModel.kt         hasPausedQuiz : StateFlow<Boolean>
│   │   │   │   │   ├── HistoryViewModel.kt      Flow<List<QuizResult>>, clearHistory()
│   │   │   │   │   ├── SettingsViewModel.kt     themeMode + soundEnabled + textSizeMode StateFlow
│   │   │   │   │   ├── AchievementsViewModel.kt achievements (StateFlow<List<AchievementState>>), newlyUnlocked (SharedFlow), resetAchievements()
│   │   │   │   │   └── FichesViewModel.kt       themes + ficheThemeProgress (StateFlow, avancement lecture X/total par thème) + markRead/resetReadFiches
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt                 Material 3 dynamique, accepte ThemeMode + TextSizeMode (échelle typo)
│   │   │   │       ├── Color.kt                 palette + SuccessGreen/FailureRed + AchievementGold partagées
│   │   │   │       └── Type.kt
│   │   │   ├── QcmFranceApplication.kt          @HiltAndroidApp
│   │   │   └── MainActivity.kt                  @AndroidEntryPoint, collecte ThemeMode + TextSizeMode
│   │   └── res/
│   │       ├── mipmap-*/                        Icônes adaptatives (fond bleu tricolore)
│   │       ├── raw/
│   │       │   ├── questions.json               258 questions de connaissances — naturalisation (seed, ids 1-999)
│   │       │   ├── questions_cr.json            questions de connaissances — carte de résident (seed, ids 2001-2999)
│   │       │   ├── questions_csp.json           questions de connaissances — carte de séjour pluriannuelle (seed, ids 3001-3999)
│   │       │   ├── situational_questions.json   80 questions de mise en situation (seed), isSituation: true — communes aux 3 QCM
│   │       │   ├── fiches.json                  Fiches thématiques officielles offline (généré par fetch_fiches.py)
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
│   └── proguard-rules.pro                       Règles Gson (TypeToken + keep complet des modèles, requis par R8 full mode)
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
    val isSituation: Boolean = false, // true = question de mise en situation (situational_questions.json)
    val variants: List<QuestionVariant> = emptyList(), // jeux de réponses alternatifs (rotation, cf. plus bas)
    val examMode: String = "NAT"    // QCM d'appartenance : "NAT" / "CR" / "CSP", ou "ALL" (mises en situation)
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
>
> `variants` (colonne `variants` ajoutée en v10, `MIGRATION_9_10`, défaut `'[]'`) porte des **jeux
> de réponses alternatifs** pour les questions de connaissances à plusieurs bonnes réponses valides.
> Room la (dé)sérialise en JSON via `Converters` (`@TypeConverters`). Voir « Variantes de réponses »
> ci-dessous.
>
> `examMode` (colonne ajoutée en v12, `MIGRATION_11_12`, défaut `'NAT'`) porte le QCM d'appartenance
> de la question. Elle n'est **pas** renseignée dans les fichiers JSON : `QuestionRepository`
> l'attribue selon le fichier de seed chargé (`readSeedFile`). Les mises en situation reçoivent
> `"ALL"` et sont tirées dans les trois QCM. Toutes les requêtes filtrées par QCM utilisent le
> prédicat `(examMode = :mode OR isSituation = 1)`.

### Variantes de réponses (rotation des jeux de réponses)

Certaines questions de connaissances admettent **plusieurs bonnes réponses valides** (« Quel musée
est situé à Paris ? » → Louvre, mais aussi Orsay, Pompidou…). Chaque jeu de réponses = 1 bonne
réponse + 3 distracteurs. En **examen**, le jeu affiché **tourne aléatoirement** ; en
**entraînement**, **tous** les jeux sont déroulés séquentiellement (chaque jeu = un item).

- **Modèle** : `QuestionVariant(optionA..optionD, correctAnswer)` (POJO, pas d'entité). Les variantes
  d'une question sont stockées dans sa colonne `variants` (rattachées à l'**id de base**), pas comme
  des lignes séparées : une question à variantes **n'est donc jamais tirée plus d'une fois par examen**.
- **Seed** : champ optionnel `"variants": [ … ]` dans `questions.json` (Gson mappe le tableau imbriqué).
  Uniquement sur les questions de connaissances (les mises en situation n'en ont pas).
- **Matérialisation** : `Question.allAnswerSets()` (`ui/viewmodel/QuestionExt.kt`) renvoie la liste
  { base } ∪ `variants`, chaque jeu matérialisé dans une copie (id inchangé, `variants` vidées).
  - **Examen** : `pickVariant()` (= `allAnswerSets().random()`) choisit un jeu au hasard, **puis**
    `withShuffledOptions()`. Appelé au chargement (`QuizViewModel.startQuiz`).
  - **Entraînement** : `TrainingViewModel.startTheme` fait `flatMap { allAnswerSets() }` — tous les
    jeux apparaissent à la suite (ordre stable pour la reprise), chacun compté dans le total du
    thème (`TrainingRepository.totalForTheme` = `Σ (1 + variants.size)`). `TrainingScreen` affiche
    « Jeu de réponses X sur Y » (`training_variant_counter`) quand la question a plusieurs jeux.
- **Transparent pour le reste** : le jeu retenu est matérialisé dans l'état UI, donc scoring, écran
  résultat, pause/reprise (`PausedQuiz` sérialise l'état déjà matérialisé) et succès `exam_all_seen`
  (compté par id de base) restent corrects sans code dédié.
- **Ajout/correction** : éditer `questions.json` **puis** incrémenter `CONTENT_VERSION` (comme toute
  correction de contenu). `QUESTIONS.md` liste les bonnes réponses alternatives *(variantes : …)*.

### Entité Room — `PausedQuiz`

```kotlin
@Entity(tableName = "paused_quiz")
data class PausedQuiz(
    @PrimaryKey val id: Int = 1,           // singleton — un seul examen en pause à la fois
    val questionsJson: String,             // Gson List<Question> avec options déjà mélangées
    val answersJson: String,               // Gson Map<Int,String> (questionId → lettre)
    val currentIndex: Int,
    val remainingSeconds: Int,
    val savedAt: Long = System.currentTimeMillis(),
    val examMode: String = "NAT"           // QCM de l'examen en pause (restauré à la reprise)
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

Avancement du **mode entraînement**. La PK n'est pas le thème nu mais la clé produite par `ExamMode.trainingKey(theme)` : `"<thème>"` pour la naturalisation (clé historique), `"CR::<thème>"` / `"CSP::<thème>"` pour les autres QCM — chaque QCM a donc sa propre progression sur les mêmes 5 thèmes, sans migration de schéma. `currentIndex` sert de point de reprise et de valeur « X » de la barre `X/total`. Thème terminé quand `currentIndex >= total`. La table est créée par `MIGRATION_4_5` (BDD passée en v5). Réinitialisation globale via `TrainingProgressDao.clear()` (bouton dans les Paramètres).

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
> pour séparer les cycles (par QCM, et connaissances / mises en situation) sans migrer le schéma de
> cette table, la clé est produite par `ExamMode.cycleKey(theme, isSituation)` :
> `"<thème>"` et `"<thème>::situation"` pour la naturalisation (clés historiques, la progression des
> installations existantes est préservée), `"CR::<thème>"`, `"CR::<thème>::situation"`,
> `"CSP::<thème>"`… pour les autres QCM.

### Cycle de tirage de l'examen

`QuestionRepository.drawIdsFromCycle(theme, count, isSituation, mode)` remplace l'ancien tirage `ORDER BY RANDOM()` :

1. Calcule la clé de cycle (`ExamMode.cycleKey()`, cf. ci-dessus) et charge la permutation persistée (`exam_cycle.orderJson`) et son curseur pour cette clé ; si absente, ou si l'ensemble des ids ne correspond plus (questions ajoutées/supprimées), génère une nouvelle permutation aléatoire et repart du curseur 0.
2. Prend les `count` ids suivants à partir du curseur, en avançant le curseur.
3. Si la permutation est épuisée avant d'avoir pris `count` ids (fin d'un tour), génère une nouvelle permutation de l'ensemble des ids du thème/type pour le tour suivant — en plaçant les ids déjà pris dans ce tirage en fin de liste, pour ne pas les retirer immédiatement dans le même examen.
4. Persiste la permutation (éventuellement renouvelée) et le nouveau curseur, sous la clé de cycle.

`drawStratifiedQuestions(mode)` appelle cette fonction une fois par thème pour `connaissanceCounts` (28 au total, `isSituation = false`) puis une fois par thème pour `situationCounts` (12 au total, `isSituation = true`). Les 40 questions chargées sont ensuite partitionnées sur `isSituation` et rendues dans l'ordre « connaissances mélangées, puis mises en situation mélangées » — ce partitionnement rétablit aussi un ordre déterminé, `getByIds` ne préservant pas l'ordre de la liste d'ids.

Résultat : toutes les questions d'un QCM/thème/type sont utilisées une fois avant qu'une répétition ne survienne d'un examen à l'autre, et passer un examen CR ne consomme pas la rotation de la naturalisation. Indépendant du flux pause/reprise (`PausedQuiz`) : le cycle n'avance qu'au lancement d'un nouvel examen (`QuizViewModel.startQuiz(mode)` → `drawStratifiedQuestions(mode)`), jamais pendant une reprise.

> **Garde-fou.** Si un thème comptait moins de questions que le tirage n'en demande (ajout d'un futur QCM, thème amputé), `drawIdsFromCycle` en fournirait autant qu'il peut **sans jamais tirer deux fois la même** : l'examen serait plus court que 40 questions plutôt que faussé par un doublon dédoublonné en base. `scripts/check_questions_consistency.py` signale ce cas. Les trois listes actuelles sont complètes, le cas ne se produit pas.

---

## Mode « S'entraîner » (entraînement)

Mode complémentaire à l'examen, orienté apprentissage — l'inverse UX de l'examen.

| Aspect | Examen | Entraînement |
|---|---|---|
| Questions | 40 tirées aléatoirement dans le QCM choisi | toutes les questions d'**un** thème d'**un** QCM |
| Ordre | 28 connaissances mélangées, **puis** 12 mises en situation mélangées | **fixe** (`ORDER BY id`) pour une reprise cohérente |
| Chronomètre | 45 min | aucun |
| Feedback | uniquement à la fin | **immédiat** après chaque réponse |
| Source | — | **lien cliquable** (`Question.source`) + explication, dans tous les cas |
| Avancement | pause/reprise (1 examen) | **par QCM + thème**, persisté à chaque question |

- **Flux** : Accueil → « S'entraîner » → `TrainingThemesScreen` (**3 sections, une par QCM**, chacune avec ses 5 thèmes + barre `X/total`) → choix d'un thème → `TrainingScreen`.
- **Persistance** : `TrainingViewModel.next()` enregistre `currentIndex` après chaque question via `TrainingRepository.saveProgress(mode, theme, index)`. Aucun mécanisme « pause » nécessaire : un simple retour ne perd rien.
- **Feedback** : sélection (`selectAnswer`, modifiable) → bouton **« Confirmer »** (`confirmAnswer`) → `revealed=true`, l'option correcte passe en vert, une mauvaise réponse sélectionnée en rouge ; bloc « Bonne/Mauvaise réponse » + `explanation` (si non vide) + bouton « Voir la source » (`LocalUriHandler.openUri`). Tant que la réponse n'est pas confirmée, la correction reste cachée et la sélection peut être changée. Après confirmation, le bouton bas devient « Suivant »/« Terminer ».
- **Seed partagé** : `QuestionRepository.seedIfNeeded()` (extrait de `drawStratifiedQuestions()`) est appelé aussi par le chemin entraînement, pour le cas où l'utilisateur ouvre l'entraînement avant tout examen.
- **Mises en situation incluses** : `TrainingRepository.questionsForTheme(mode, theme)`/`totalForTheme(mode, theme)` s'appuient sur `QuestionDao.getAllByTheme(theme, mode)`, qui retient les connaissances du QCM **et** toutes les mises en situation (`isSituation = 1`) : elles apparaissent donc dans l'entraînement des trois QCM, sans code de filtrage dédié.
- **Variantes déroulées** : chaque question à variantes apparaît une fois **par jeu de réponses** (items consécutifs de même id, libellé « Jeu de réponses X sur Y ») ; `totalForTheme()` compte `Σ (1 + variants.size)` pour rester cohérent avec cette expansion (barre `X/total` et complétion).
- **Option shuffling** : réutilise `withShuffledOptions()` (déplacé dans `ui/viewmodel/QuestionExt.kt`, partagé par les deux ViewModels).
- **Réinitialisation** : Paramètres → « Réinitialiser la progression » (AlertDialog de confirmation) → `TrainingViewModel.resetTraining()` → `TrainingRepository.resetAll()`.

---

## Système de succès (« Achievements »)

Système de gamification inspiré des trophées de jeux vidéo. Catalogue statique en code, état débloqué persisté en Room, popup au déblocage, page dédiée depuis l'accueil.

### Catalogue (27 succès, `Achievements.ALL`)

**Examen (6)** — les trois premiers sont communs aux QCM (peu importe lequel a été passé) :

| id | Titre | Condition | Cible |
|---|---|---|---|
| `exam_first_completed` | Premier pas républicain | Terminer un 1er examen | — |
| `exam_first_passed` | Reçu ! | Réussir un examen (≥ 32/40) | — |
| `exam_perfect` *(rare)* | Sans-faute | Score parfait 40/40 | — |
| `exam_all_seen` | Tour complet | Toutes les questions **naturalisation** vues ≥ 1 fois en examen | X/total (dynamique) |
| `exam_all_seen_cr` | Tour complet — carte de résident | Idem pour le QCM CR | X/total (dynamique) |
| `exam_all_seen_csp` | Tour complet — séjour pluriannuel | Idem pour le QCM CSP | X/total (dynamique) |

**Entraînement (18)** — un bloc identique de 6 succès par QCM, généré depuis `TRAINING_BLOCKS` :

| ids | Condition | Cible |
|---|---|---|
| `train_{principes,institutions,droits,histoire,societe}` + `train_all` *(rare)* | Thèmes d'entraînement de la **naturalisation** | — / X/5 |
| `train_cr_*` + `train_cr_all` *(rare)* | Thèmes d'entraînement de la **carte de résident** | — / X/5 |
| `train_csp_*` + `train_csp_all` *(rare)* | Thèmes d'entraînement de la **carte de séjour pluriannuelle** | — / X/5 |

**Fiches (3)** : `fiche_first_read` (1re fiche), `fiche_30_read` (X/30), `fiche_all_read` *(rare,
X/total dynamique)* — communes aux trois QCM.

Chaque succès : `id`, `titleRes`/`descriptionRes` (strings.xml), `emoji`, `category`
(EXAM/TRAINING/TRAINING_CR/TRAINING_CSP/FICHES — chaque catégorie porte son `titleRes`, l'ordre de
l'enum est l'ordre des sections de l'écran), `rarity` (COMMON/RARE — les rares ont un liseré doré et
une description masquée « secret » tant que verrouillés), `target` (>1 ⇒ succès à progression,
barre `X/target`).

> Les ids du bloc naturalisation et celui de `exam_all_seen` sont **inchangés** depuis l'avant
> multi-QCM : les succès déjà débloqués par les utilisateurs restent acquis.

### Architecture

- **`AchievementRecord`** (`@Entity` `achievements`, PK=`id`) : `unlockedAt` (nullable — null = non débloqué) + `progress`.
- **`SeenQuestion`** (`@Entity` `seen_question`, PK=`questionId`) : ids des questions déjà vues en examen, tous QCM confondus (les ids sont uniques). La progression d'un `exam_all_seen_*` se lit par jointure (`QuestionDao.countSeenForMode`), sa cible par `QuestionDao.countForMode` — un examen CR ne fait donc pas progresser le succès naturalisation.
- **`ReadFiche`** (`@Entity` `read_fiche`, PK=`ficheId`) : ids des fiches déjà consultées ; `COUNT(*)` = progression des succès fiches (et source de la barre X/total de lecture par thème). Table créée par `MIGRATION_10_11` (BDD v10 → v11).
- **`MIGRATION_8_9`** (BDD v8 → v9) crée les tables `achievements` + `seen_question`.
- **`AchievementRepository`** (`@Singleton`, injecte aussi `FichesRepository`) : point d'entrée idempotent.
  - `unlock(id)` : débloque un succès tout-ou-rien, sans ré-émettre si déjà débloqué.
  - `onExamCompleted(mode, passed, perfect, questionIds)` : appelé par `QuizViewModel.submitQuiz()`. Débloque `exam_first_completed`, et conditionnellement `exam_first_passed` / `exam_perfect` ; insère les ids vus (`seen_question`) et met à jour le `exam_all_seen_*` du QCM passé (cible = `questionDao.countForMode(mode)`).
  - `onThemeCompleted(mode, theme)` : appelé par `TrainingViewModel` à la fin d'un thème (`next()`) **et** au rattrapage à l'ouverture d'un thème déjà terminé (`startTheme`). Résout le succès via `Achievements.TRAINING_BY_KEY[mode.trainingKey(theme)]` (même clé que `training_progress`), le débloque, puis met à jour le `train_*_all` du QCM (progression = nombre de ses thèmes terminés).
  - `onFicheRead(readCount, totalFiches)` : appelé par `FichesViewModel.markRead()` à l'ouverture d'une fiche. Débloque `fiche_first_read`, met à jour `fiche_30_read` (cible 30) et `fiche_all_read` (cible = `fichesRepository.totalFichesCount()`, résolue dynamiquement dans `observe()` comme les `exam_all_seen_*`).
  - `resetAll()` (bouton « Réinitialiser les succès ») vide `achievements` + `seen_question` ; il **ne** touche **pas** `read_fiche` (remis à zéro par « Réinitialiser la progression », cf. mode S'entraîner).
  - `newlyUnlocked: SharedFlow<Achievement>` : émet chaque nouveau déblocage.
  - `observe(): Flow<List<AchievementState>>` : catalogue + état persisté, pour la page.
  - `resetAll()` : vide `achievements` + `seen_question` (bouton Paramètres).
- **Popup** : `AchievementUnlockedBanner` (overlay dans `QcmNavGraph`, au-dessus du `NavHost`) collecte `newlyUnlocked` dans une file ; bandeau qui glisse du haut (`AnimatedVisibility`), liseré doré si rare, auto-dismiss ~4 s ou au clic.
- **Page** : `AchievementsScreen` (route `achievements`, bouton « 🏆 Succès » sur l'accueil) : compteur `X/27` (calculé sur `states.size`), succès groupés par catégorie (itération sur `AchievementCategory.entries`), verrouillés grisés (🔒), barres de progression.

> **Rattrapage** : les succès de thème d'entraînement se débloquent aussi en rouvrant un thème déjà
> terminé avant l'ajout de la fonctionnalité (`onThemeCompleted` idempotent dans `startTheme`).
> En revanche les `exam_all_seen_*` ne comptent que les examens soumis **après** la mise à jour.

---

## Fiches thématiques officielles (consultation hors-ligne)

Dump embarqué des **fiches par thématiques** officielles de
`formation-civique.interieur.gouv.fr` (les 5 thèmes → arborescence complète des sous-fiches),
consultable **sans réseau ni permission** (règle 10). Intégré au mode **S'entraîner**.

- **Données** : `res/raw/fiches.json` (schéma `FichesData` → `FicheTheme` → `Fiche{id,title,url,markdown}`),
  lu par `FichesRepository` via Gson + **cache mémoire**. Le contenu des fiches n'est pas persisté en
  Room (**pas de `CONTENT_VERSION`**, relu du raw à chaque lancement) ; seul le **suivi de lecture**
  l'est (table `read_fiche`, cf. plus bas). `id` unique sur tout le dataset :
  `"<slug-thème>__<slug-fiche>"` (ASCII, sûr comme argument de navigation).
- **UI** : entrée depuis l'écran **« S'entraîner »** (`TrainingThemesScreen`, section « S'entraîner
  avec les sources officielles » : 5 cartes de thèmes avec barre de lecture `X/total`) →
  `FichesListScreen` (fiches du thème) → `FicheDetailScreen` (rendu markdown + « Voir la fiche en
  ligne » + mention source). Le contenu markdown est rendu **nativement** en Compose par `MarkdownText`
  (titres, paragraphes, listes, gras/italique, liens) — aucune WebView ni dépendance ajoutée.
- **Suivi de lecture** : ouvrir `FicheDetailScreen` marque la fiche lue (`FichesViewModel.markRead()`
  → `read_fiche`, INSERT IGNORE — comptée une seule fois). Alimente la barre `X/total` par thème
  (`FichesViewModel.ficheThemeProgress`), la **distinction lu/non lu** dans `FichesListScreen`
  (`FichesViewModel.readFicheIds` : carte atténuée `surfaceVariant` + coche verte pour une fiche lue,
  carte `primaryContainer` pour une fiche à lire) et les 3 succès « Fiches officielles ». Remise à
  zéro par « Réinitialiser la progression » (Paramètres), avec la progression d'entraînement.
- **Génération du contenu** : `scripts/fetch_fiches.py` (Python : `requests` + `beautifulsoup4` +
  `markdownify`) parcourt **récursivement** le sous-arbre de chaque thème (`crawl_theme_leaves`) et ne
  retient que les **feuilles de contenu** : une page ayant des sous-pages (index de thème, pages de
  catégorie listant des liens « Pages ») est traversée mais **pas émise** ; seules les pages sans
  enfant (vrai contenu) deviennent des fiches. Extrait le contenu principal (heuristique générique
  `<main>`/`<article>`) → markdown, `id` = `"<thème>__<chemin-relatif>"`, et écrit `fiches.json`.
  > ⚠️ Le domaine gouvernemental est **bloqué par la politique réseau** des sessions Claude Code : le
  > scraping ne tourne **que** dans la pipeline GitHub Actions (runners non bloqués).
- **Pipeline** : `.github/workflows/update-fiches.yml` — **manuelle** (`workflow_dispatch`). Lance le
  script, archive le HTML brut en artifact (debug sélecteurs), puis **ouvre une PR** avec le `fiches.json`
  régénéré (relecture avant merge). Si 0 fiche extraite → site probablement rendu en JS → activer le
  fallback Playwright documenté dans `fetch_fiches.py`.
- **Attribution** : contenu ministère de l'Intérieur (information publique, Licence Ouverte/Etalab).
  Lien source + mention affichés sur chaque fiche.

---

### Format JSON de seed (`res/raw/questions*.json`, `res/raw/situational_questions.json`)

Même schéma dans les quatre fichiers ; seul `situational_questions.json` renseigne
`"isSituation": true` sur chaque entrée (absent des fichiers de connaissances, donc `false` par
défaut). **`examMode` n'apparaît dans aucun fichier** : il est attribué au chargement selon le
fichier lu (`QuestionRepository.readSeedFile`).

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
    "optionA": "Elle dispense l'élève de la séance concernée mais maintient l'évaluation, sur simple courrier des parents",
    "optionB": "Elle rappelle que les programmes scolaires nationaux s'appliquent à tous les élèves, sans dispense pour motif religieux",
    "optionC": "Elle convoque un conseil de discipline, la demande valant refus d'assiduité de la part de la famille",
    "optionD": "Elle propose un cours adapté aux convictions de chaque famille, comme pour les repas à la cantine",
    "correctAnswer": "B",
    "explanation": "L'école publique est laïque : les programmes nationaux s'imposent à tous les élèves. Une dispense n'est possible que pour des motifs médicaux, jamais religieux. Aucun aménagement confessionnel du programme n'est prévu, et l'analogie avec les menus de substitution à la cantine ne tient pas : ceux-ci sont une simple faculté de la collectivité, pas un droit.",
    "source": "https://www.education.gouv.fr/",
    "isSituation": true
  }
]
```

> `correctAnswers` reste présent dans `questions.json` (donnée de référence) mais n'est **pas**
> chargé par l'app : le champ n'existe plus dans l'entité Room et Gson ignore les clés inconnues.
> Les plages d'ids sont **réservées par fichier** pour éviter toute collision : 1-999
> (naturalisation, 1-258 utilisés), 1000-1999 (mises en situation, 1001-1080 utilisés),
> 2001-2999 (CR), 3001-3999 (CSP). `scripts/check_questions_consistency.py` vérifie ces plages.

---

## DAO

Le prédicat `(examMode = :mode OR isSituation = 1)` retient les connaissances du QCM demandé **et**
les mises en situation, communes aux trois.

```kotlin
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE theme = :theme AND (examMode = :mode OR isSituation = 1) ORDER BY id")
    suspend fun getAllByTheme(theme: String, mode: String): List<Question>

    @Query("SELECT id FROM questions WHERE theme = :theme AND isSituation = :isSituation AND (examMode = :mode OR isSituation = 1) ORDER BY id")
    suspend fun getIdsByTheme(theme: String, isSituation: Boolean, mode: String): List<Int>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    /** Cible du succès « Tour complet » du QCM. */
    @Query("SELECT COUNT(*) FROM questions WHERE examMode = :mode OR isSituation = 1")
    suspend fun countForMode(mode: String): Int

    /** Progression du succès « Tour complet » du QCM (jointure avec seen_question). */
    @Query("""
        SELECT COUNT(*) FROM seen_question s
        INNER JOIN questions q ON q.id = s.questionId
        WHERE q.examMode = :mode OR q.isSituation = 1
    """)
    suspend fun countSeenForMode(mode: String): Int
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
    val isLoading: Boolean = true,                 // spinner pendant le chargement des questions
    val mode: ExamMode = ExamMode.DEFAULT          // QCM passé (vivier de questions + libellé affiché)
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

`startQuiz(mode)` réinitialise l'état de façon **synchrone** (un `isFinished=true` résiduel déclencherait
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
| `home` | Accueil | Titre, **sélecteur de QCM** (Naturalisation / Carte de résident / Carte de séjour pluriannuelle, persisté), boutons "Commencer" / "Reprendre" (conditionnel), "S'entraîner", "Historique", "Succès", "Paramètres", icônes "Ressources" / "Aide" |
| `quiz` | Examen | Question N/40, 4 options, chrono MM:SS, bouton Pause, BackHandler, barre de progression, son |
| `result` | Résultat | Score X/40, temps utilisé, mention Réussi/Échoué, détail, export |
| `history` | Historique | Liste des résultats passés (avec le QCM de chacun), export individuel, vider l'historique |
| `settings` | Paramètres | Thème (Système/Clair/Sombre), toggle son, réinitialiser la progression d'entraînement, réinitialiser le cycle de l'examen, réinitialiser les succès, accès « À propos » |
| `help` | Aide | Guide utilisateur, **les trois examens couverts**, règles de l'examen, thèmes, fonctionnalités (les liens officiels sont désormais sur `resources`) |
| `resources` | Ressources complémentaires | Liens officiels cliquables (navigateur) en 3 sections (« Textes officiels », « Examen civique et tests », « Fiches thématiques officielles ») — liens en ligne uniquement (l'accès hors-ligne est passé sur `training_themes`) — accès par l'icône « liste » de l'accueil |
| `about` | À propos / Mises à jour | Version installée (lue via `PackageManager`, sans réseau) + bouton ouvrant `github.com/borisboiakki/qcm_france/releases/latest` dans le navigateur pour télécharger l'APK — aucune permission ajoutée |
| `training_themes` | S'entraîner | **3 sections (une par QCM)** de 5 cartes `X/total`, puis section « S'entraîner avec les sources officielles » : 5 cartes de fiches avec barre de lecture `X/total` → `fiches_list/{theme}` |
| `training` | Entraînement (question) | Question d'un thème, feedback immédiat (vert/rouge), explication + lien source, "Suivant"/"Terminer" |
| `achievements` | Succès | Liste des succès groupés par catégorie (Examen / Entraînement / Fiches), verrouillés grisés, barres de progression `X/target`, dates de déblocage |
| `fiches_list/{theme}` | Fiches (liste) | Liste des fiches d'un thème (argument `theme` encodé) |
| `fiche_detail/{ficheId}` | Fiche (détail) | Rendu markdown de la fiche (`MarkdownText`) + bouton « Voir en ligne » + mention source |

**Règle UX importante :** sur l'écran quiz, **aucun feedback immédiat** sur la bonne/mauvaise réponse (c'est un examen, pas un entraînement). Le feedback n'est affiché qu'à l'écran résultat.

---

## Pré-peuplement et synchronisation de la base de données

La BDD est peuplée **au premier lancement** depuis les quatre fichiers de seed (`questions.json`,
`questions_cr.json`, `questions_csp.json`, `situational_questions.json`), directement dans
`QuestionRepository.seedIfNeeded()` (appelée par `drawStratifiedQuestions(mode)` et par le chemin
entraînement). La même fonction **resynchronise**
aussi le contenu quand le JSON a été corrigé, sans réinstallation ni migration Room :

```kotlin
// QuestionRepository.kt
companion object {
    const val CONTENT_VERSION = 4          // à incrémenter à CHAQUE correction d'un fichier JSON
    private const val CONTENT_PREFS = "question_content"
    private const val KEY_CONTENT_VERSION = "content_version"
}

suspend fun seedIfNeeded() {
    val prefs = context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
    val appliedVersion = prefs.getInt(KEY_CONTENT_VERSION, 0)
    val isEmpty = dao.count() == 0
    if (!isEmpty && appliedVersion >= CONTENT_VERSION) return   // à jour → rien à faire

    // readSeedFile() étiquette chaque question avec le QCM correspondant au fichier lu, et
    // reconstruit les instances par le constructeur (Gson les alloue sans l'appeler : les champs
    // absents du JSON, comme `variants`, sont null malgré leur type non-null).
    val questions = listOf(
        R.raw.questions             to ExamMode.NATURALISATION.code,
        R.raw.questions_cr          to ExamMode.RESIDENT_CARD.code,
        R.raw.questions_csp         to ExamMode.MULTI_YEAR_PERMIT.code,
        R.raw.situational_questions to ExamMode.SHARED_CODE          // "ALL"
    ).flatMap { (resId, mode) -> readSeedFile(resId, mode) }

    dao.insertAll(questions)   // REPLACE : upsert par id (seed OU resynchro)
    prefs.edit().putInt(KEY_CONTENT_VERSION, CONTENT_VERSION).apply()
}
```

> **Seed vs resynchro.** Premier lancement (`count() == 0`) → insertion complète. Contenu obsolète
> (version appliquée < `CONTENT_VERSION`) → ré-application du JSON en `INSERT OR REPLACE` (upsert par
> id) : les libellés corrigés et les nouvelles questions arrivent, **sans toucher** aux tables
> `quiz_result`, `achievements`, `training_progress`, `exam_cycle`… Les installs antérieures à cette
> fonctionnalité n'ont pas de version stockée (défaut `0`) → resynchronisation unique automatique.
>
> **Workflow de correction de contenu :** modifier le(s) fichier(s) JSON, lancer
> `python3 scripts/check_questions_consistency.py` **puis incrémenter `CONTENT_VERSION`**. Sans ce bump, les apps déjà installées ne verraient pas le changement (la BDD
> Room n'est plus relue depuis le JSON après le seed initial). La version appliquée est stockée en
> `SharedPreferences` (`question_content` / `content_version`), donc aucune migration Room n'est
> nécessaire pour une simple correction de contenu.

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
  ├─ Sélecteur de QCM : Naturalisation / Carte de résident / Carte de séjour pluriannuelle
  │     (persisté en DataStore ; détermine le vivier de questions de l'examen lancé)
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
  ├─ "🏆 Succès" ──► [AchievementsScreen]
  │                     ├─ Compteur X/27 + barre globale
  │                     ├─ Succès groupés (Examen / Entraînement ×3 QCM / Fiches), verrouillés grisés (🔒)
  │                     └─ Barres de progression X/target (exam_all_seen_*, train_*_all, fiche_30_read, fiche_all_read)
  │   (popup « Succès débloqué ! » affiché en overlay quel que soit l'écran)
  ├─ "Paramètres" ──► [SettingsScreen]
  │                     ├─ Thème : Système / Clair / Sombre (persisté DataStore)
  │                     ├─ Taille du texte : Petit / Moyen / Grand (slider, persisté DataStore)
  │                     ├─ Son de sélection : activé/désactivé (persisté DataStore)
  │                     ├─ "Réinitialiser la progression" → TrainingProgressDao.clear() + read_fiche (lecture des fiches)
  │                     ├─ "Réinitialiser le cycle de l'examen" → ExamCycleDao.clear()
  │                     ├─ "Réinitialiser les succès" → AchievementRepository.resetAll()
  │                     └─ "Mises à jour et à propos" ──► [AboutScreen]
  │                                                        ├─ Version installée (PackageManager)
  │                                                        └─ "Télécharger la dernière version sur GitHub" → navigateur (releases/latest)
  ├─ Icône Ressources (liste) ──► [ResourcesScreen]
  │                            └─ liens officiels en ligne cliquables (gouvernement, Légifrance, etc.)
  └─ Icône Aide (Info) ──► [HelpScreen]
                            └─ Règles de l'examen, thèmes, fonctionnalités
```

---

## Commandes utiles

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint

# Contenu des questions (3 QCM + mises en situation)
python3 scripts/check_questions_consistency.py   # ids, propositions, variantes, cohérence inter-QCM
python3 scripts/generate_questions_md.py         # régénère QUESTIONS.md
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
