package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.qcmfrance.R

/**
 * Catégorie d'un succès, pour le regroupement à l'écran « Succès ». L'ordre des constantes est
 * l'ordre d'affichage des sections. Chaque QCM a sa propre section d'entraînement.
 */
enum class AchievementCategory(val titleRes: Int) {
    EXAM(R.string.achievements_category_exam),
    TRAINING(R.string.achievements_category_training),
    TRAINING_CR(R.string.achievements_category_training_cr),
    TRAINING_CSP(R.string.achievements_category_training_csp),
    FICHES(R.string.achievements_category_fiches)
}

/** Rareté d'un succès (inspiration jeux vidéo) : les succès rares ont un rendu doré + « secret » tant que verrouillés. */
enum class AchievementRarity { COMMON, RARE }

/**
 * Définition (statique) d'un succès. Le catalogue vit dans le binaire ([Achievements.ALL]) ;
 * seul l'état débloqué / la progression sont persistés en base ([AchievementRecord]).
 *
 * @param target cible pour les succès à progression (barre X/target). 1 = succès « tout ou rien ».
 *               Pour les succès « Tour complet » ([Achievements.EXAM_ALL_SEEN_BY_MODE]), la cible
 *               réelle est résolue dynamiquement (nombre de questions du QCM) par le repository.
 */
data class Achievement(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val emoji: String,
    val category: AchievementCategory,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val target: Int = 1
)

/**
 * État d'un succès pour l'UI : sa définition + l'état persisté (débloqué, date, progression).
 * [target] peut différer de [Achievement.target] pour les succès à cible dynamique.
 */
data class AchievementState(
    val achievement: Achievement,
    val unlockedAt: Long?,
    val progress: Int,
    val target: Int
) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val hasProgressBar: Boolean get() = target > 1
    /** Description cachée : succès rare encore verrouillé → effet « trophée secret ». */
    val isSecret: Boolean get() = !isUnlocked && achievement.rarity == AchievementRarity.RARE
}

/**
 * État débloqué / progression d'un succès, persisté en Room. Une ligne n'existe que si le succès
 * a déjà été débloqué **ou** a une progression en cours ([unlockedAt] null tant que non débloqué).
 */
@Entity(tableName = "achievements")
data class AchievementRecord(
    @PrimaryKey val id: String,
    val unlockedAt: Long?,
    val progress: Int = 0
)

/** Catalogue statique des succès + helpers. */
object Achievements {

    // --- ids (stables : servent de clés en base — ne jamais renommer) ---
    const val EXAM_FIRST_COMPLETED = "exam_first_completed"
    const val EXAM_FIRST_PASSED    = "exam_first_passed"
    const val EXAM_PERFECT         = "exam_perfect"
    // « Tour complet » : un succès par QCM. L'id du mode naturalisation est celui d'avant le
    // multi-QCM, pour que les déblocages déjà obtenus restent acquis.
    const val EXAM_ALL_SEEN        = "exam_all_seen"
    const val EXAM_ALL_SEEN_CR     = "exam_all_seen_cr"
    const val EXAM_ALL_SEEN_CSP    = "exam_all_seen_csp"
    const val FICHE_FIRST_READ     = "fiche_first_read"
    const val FICHE_30_READ        = "fiche_30_read"
    const val FICHE_ALL_READ       = "fiche_all_read"

    // --- noms officiels des thèmes (doivent correspondre à QuestionRepository) ---
    private const val THEME_PRINCIPES    = "Principes et valeurs de la République"
    private const val THEME_INSTITUTIONS = "Système institutionnel et politique"
    private const val THEME_DROITS       = "Droits et devoirs"
    private const val THEME_HISTOIRE     = "Histoire, géographie et culture"
    private const val THEME_SOCIETE      = "Vivre dans la société française"

    private val THEMES = listOf(
        THEME_PRINCIPES, THEME_INSTITUTIONS, THEME_DROITS, THEME_HISTOIRE, THEME_SOCIETE
    )

    /** QCM → id de son succès « Tour complet » (toutes les questions du QCM vues en examen). */
    val EXAM_ALL_SEEN_BY_MODE: Map<ExamMode, String> = mapOf(
        ExamMode.NATURALISATION    to EXAM_ALL_SEEN,
        ExamMode.RESIDENT_CARD     to EXAM_ALL_SEEN_CR,
        ExamMode.MULTI_YEAR_PERMIT to EXAM_ALL_SEEN_CSP
    )

    /**
     * Définition d'un bloc de succès d'entraînement pour un QCM : 5 succès de thème + le succès
     * « tous les thèmes ». Les ids du mode naturalisation sont ceux d'avant le multi-QCM.
     */
    private data class TrainingBlock(
        val mode: ExamMode,
        val category: AchievementCategory,
        val themeIds: List<String>,
        val allId: String,
        val allTitleRes: Int,
        val allDescRes: Int,
        val allEmoji: String,
        val themeTitleRes: List<Int>,
        val themeDescRes: List<Int>
    )

    private val THEME_EMOJIS = listOf("⚖️", "🏛️", "📜", "🗺️", "🤝")

    private val TRAINING_BLOCKS = listOf(
        TrainingBlock(
            mode = ExamMode.NATURALISATION,
            category = AchievementCategory.TRAINING,
            themeIds = listOf(
                "train_principes", "train_institutions", "train_droits",
                "train_histoire", "train_societe"
            ),
            allId = "train_all",
            allTitleRes = R.string.ach_train_all_title,
            allDescRes = R.string.ach_train_all_desc,
            allEmoji = "🎓",
            themeTitleRes = listOf(
                R.string.ach_train_principes_title, R.string.ach_train_institutions_title,
                R.string.ach_train_droits_title, R.string.ach_train_histoire_title,
                R.string.ach_train_societe_title
            ),
            themeDescRes = listOf(
                R.string.ach_train_principes_desc, R.string.ach_train_institutions_desc,
                R.string.ach_train_droits_desc, R.string.ach_train_histoire_desc,
                R.string.ach_train_societe_desc
            )
        ),
        TrainingBlock(
            mode = ExamMode.RESIDENT_CARD,
            category = AchievementCategory.TRAINING_CR,
            themeIds = listOf(
                "train_cr_principes", "train_cr_institutions", "train_cr_droits",
                "train_cr_histoire", "train_cr_societe"
            ),
            allId = "train_cr_all",
            allTitleRes = R.string.ach_train_cr_all_title,
            allDescRes = R.string.ach_train_cr_all_desc,
            allEmoji = "🪪",
            themeTitleRes = listOf(
                R.string.ach_train_cr_principes_title, R.string.ach_train_cr_institutions_title,
                R.string.ach_train_cr_droits_title, R.string.ach_train_cr_histoire_title,
                R.string.ach_train_cr_societe_title
            ),
            themeDescRes = listOf(
                R.string.ach_train_cr_principes_desc, R.string.ach_train_cr_institutions_desc,
                R.string.ach_train_cr_droits_desc, R.string.ach_train_cr_histoire_desc,
                R.string.ach_train_cr_societe_desc
            )
        ),
        TrainingBlock(
            mode = ExamMode.MULTI_YEAR_PERMIT,
            category = AchievementCategory.TRAINING_CSP,
            themeIds = listOf(
                "train_csp_principes", "train_csp_institutions", "train_csp_droits",
                "train_csp_histoire", "train_csp_societe"
            ),
            allId = "train_csp_all",
            allTitleRes = R.string.ach_train_csp_all_title,
            allDescRes = R.string.ach_train_csp_all_desc,
            allEmoji = "📇",
            themeTitleRes = listOf(
                R.string.ach_train_csp_principes_title, R.string.ach_train_csp_institutions_title,
                R.string.ach_train_csp_droits_title, R.string.ach_train_csp_histoire_title,
                R.string.ach_train_csp_societe_title
            ),
            themeDescRes = listOf(
                R.string.ach_train_csp_principes_desc, R.string.ach_train_csp_institutions_desc,
                R.string.ach_train_csp_droits_desc, R.string.ach_train_csp_histoire_desc,
                R.string.ach_train_csp_societe_desc
            )
        )
    )

    /**
     * Clé d'entraînement ([ExamMode.trainingKey]) → id du succès de thème correspondant. La clé est
     * exactement celle utilisée comme PK dans `training_progress`, ce qui évite tout second
     * encodage du couple (QCM, thème).
     */
    val TRAINING_BY_KEY: Map<String, String> = TRAINING_BLOCKS.flatMap { block ->
        THEMES.mapIndexed { i, theme -> block.mode.trainingKey(theme) to block.themeIds[i] }
    }.toMap()

    /** QCM → id de son succès « tous les thèmes d'entraînement terminés ». */
    val TRAINING_ALL_BY_MODE: Map<ExamMode, String> =
        TRAINING_BLOCKS.associate { it.mode to it.allId }

    /** QCM → ids de ses 5 succès de thème (progression du succès « tous les thèmes »). */
    val TRAINING_THEME_IDS_BY_MODE: Map<ExamMode, List<String>> =
        TRAINING_BLOCKS.associate { it.mode to it.themeIds }

    val ALL: List<Achievement> = buildList {
        // --- Examen (communs aux trois QCM, sauf « Tour complet » qui est par QCM) ---
        add(
            Achievement(
                id = EXAM_FIRST_COMPLETED,
                titleRes = R.string.ach_exam_first_completed_title,
                descriptionRes = R.string.ach_exam_first_completed_desc,
                emoji = "🎯",
                category = AchievementCategory.EXAM
            )
        )
        add(
            Achievement(
                id = EXAM_FIRST_PASSED,
                titleRes = R.string.ach_exam_first_passed_title,
                descriptionRes = R.string.ach_exam_first_passed_desc,
                emoji = "✅",
                category = AchievementCategory.EXAM
            )
        )
        add(
            Achievement(
                id = EXAM_PERFECT,
                titleRes = R.string.ach_exam_perfect_title,
                descriptionRes = R.string.ach_exam_perfect_desc,
                emoji = "🏆",
                category = AchievementCategory.EXAM,
                rarity = AchievementRarity.RARE
            )
        )
        add(
            Achievement(
                id = EXAM_ALL_SEEN,
                titleRes = R.string.ach_exam_all_seen_title,
                descriptionRes = R.string.ach_exam_all_seen_desc,
                emoji = "🔄",
                category = AchievementCategory.EXAM,
                target = 338   // cible par défaut ; résolue dynamiquement au runtime
            )
        )
        add(
            Achievement(
                id = EXAM_ALL_SEEN_CR,
                titleRes = R.string.ach_exam_all_seen_cr_title,
                descriptionRes = R.string.ach_exam_all_seen_cr_desc,
                emoji = "🔄",
                category = AchievementCategory.EXAM,
                target = 289   // cible par défaut ; résolue dynamiquement au runtime
            )
        )
        add(
            Achievement(
                id = EXAM_ALL_SEEN_CSP,
                titleRes = R.string.ach_exam_all_seen_csp_title,
                descriptionRes = R.string.ach_exam_all_seen_csp_desc,
                emoji = "🔄",
                category = AchievementCategory.EXAM,
                target = 271   // cible par défaut ; résolue dynamiquement au runtime
            )
        )

        // --- Entraînement : un bloc identique par QCM (5 thèmes + « tous les thèmes ») ---
        for (block in TRAINING_BLOCKS) {
            THEMES.indices.forEach { i ->
                add(
                    Achievement(
                        id = block.themeIds[i],
                        titleRes = block.themeTitleRes[i],
                        descriptionRes = block.themeDescRes[i],
                        emoji = THEME_EMOJIS[i],
                        category = block.category
                    )
                )
            }
            add(
                Achievement(
                    id = block.allId,
                    titleRes = block.allTitleRes,
                    descriptionRes = block.allDescRes,
                    emoji = block.allEmoji,
                    category = block.category,
                    rarity = AchievementRarity.RARE,
                    target = THEMES.size
                )
            )
        }

        // --- Fiches officielles (consultation hors-ligne, communes aux trois QCM) ---
        add(
            Achievement(
                id = FICHE_FIRST_READ,
                titleRes = R.string.ach_fiche_first_read_title,
                descriptionRes = R.string.ach_fiche_first_read_desc,
                emoji = "📖",
                category = AchievementCategory.FICHES
            )
        )
        add(
            Achievement(
                id = FICHE_30_READ,
                titleRes = R.string.ach_fiche_30_read_title,
                descriptionRes = R.string.ach_fiche_30_read_desc,
                emoji = "📚",
                category = AchievementCategory.FICHES,
                target = 30
            )
        )
        add(
            Achievement(
                id = FICHE_ALL_READ,
                titleRes = R.string.ach_fiche_all_read_title,
                descriptionRes = R.string.ach_fiche_all_read_desc,
                emoji = "🧠",
                category = AchievementCategory.FICHES,
                rarity = AchievementRarity.RARE,
                target = 100   // cible par défaut ; résolue dynamiquement au runtime (nombre de fiches)
            )
        )
    }

    private val byIdMap: Map<String, Achievement> = ALL.associateBy { it.id }

    fun byId(id: String): Achievement? = byIdMap[id]
}
