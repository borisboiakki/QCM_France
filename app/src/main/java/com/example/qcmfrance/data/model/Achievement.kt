package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.qcmfrance.R

/** Catégorie d'un succès, pour le regroupement à l'écran « Succès ». */
enum class AchievementCategory { EXAM, TRAINING, FICHES }

/** Rareté d'un succès (inspiration jeux vidéo) : les succès rares ont un rendu doré + « secret » tant que verrouillés. */
enum class AchievementRarity { COMMON, RARE }

/**
 * Définition (statique) d'un succès. Le catalogue vit dans le binaire ([Achievements.ALL]) ;
 * seul l'état débloqué / la progression sont persistés en base ([AchievementRecord]).
 *
 * @param target cible pour les succès à progression (barre X/target). 1 = succès « tout ou rien ».
 *               Pour [Achievements.EXAM_ALL_SEEN], la cible réelle est résolue dynamiquement
 *               (nombre de questions en base) par le repository.
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

    // --- ids (stables : servent de clés en base) ---
    const val EXAM_FIRST_COMPLETED = "exam_first_completed"
    const val EXAM_FIRST_PASSED    = "exam_first_passed"
    const val EXAM_PERFECT         = "exam_perfect"
    const val EXAM_ALL_SEEN        = "exam_all_seen"
    const val TRAIN_PRINCIPES      = "train_principes"
    const val TRAIN_INSTITUTIONS   = "train_institutions"
    const val TRAIN_DROITS         = "train_droits"
    const val TRAIN_HISTOIRE       = "train_histoire"
    const val TRAIN_SOCIETE        = "train_societe"
    const val TRAIN_ALL            = "train_all"
    const val FICHE_FIRST_READ     = "fiche_first_read"
    const val FICHE_30_READ        = "fiche_30_read"
    const val FICHE_ALL_READ       = "fiche_all_read"

    // --- noms officiels des thèmes (doivent correspondre à QuestionRepository) ---
    private const val THEME_PRINCIPES    = "Principes et valeurs de la République"
    private const val THEME_INSTITUTIONS = "Système institutionnel et politique"
    private const val THEME_DROITS       = "Droits et devoirs"
    private const val THEME_HISTOIRE     = "Histoire, géographie et culture"
    private const val THEME_SOCIETE      = "Vivre dans la société française"

    /** Thème officiel → id du succès d'entraînement correspondant. */
    val TRAINING_BY_THEME: Map<String, String> = mapOf(
        THEME_PRINCIPES    to TRAIN_PRINCIPES,
        THEME_INSTITUTIONS to TRAIN_INSTITUTIONS,
        THEME_DROITS       to TRAIN_DROITS,
        THEME_HISTOIRE     to TRAIN_HISTOIRE,
        THEME_SOCIETE      to TRAIN_SOCIETE
    )

    val ALL: List<Achievement> = listOf(
        // Examen
        Achievement(
            id = EXAM_FIRST_COMPLETED,
            titleRes = R.string.ach_exam_first_completed_title,
            descriptionRes = R.string.ach_exam_first_completed_desc,
            emoji = "🎯",
            category = AchievementCategory.EXAM
        ),
        Achievement(
            id = EXAM_FIRST_PASSED,
            titleRes = R.string.ach_exam_first_passed_title,
            descriptionRes = R.string.ach_exam_first_passed_desc,
            emoji = "✅",
            category = AchievementCategory.EXAM
        ),
        Achievement(
            id = EXAM_PERFECT,
            titleRes = R.string.ach_exam_perfect_title,
            descriptionRes = R.string.ach_exam_perfect_desc,
            emoji = "🏆",
            category = AchievementCategory.EXAM,
            rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = EXAM_ALL_SEEN,
            titleRes = R.string.ach_exam_all_seen_title,
            descriptionRes = R.string.ach_exam_all_seen_desc,
            emoji = "🔄",
            category = AchievementCategory.EXAM,
            target = 318   // cible par défaut ; résolue dynamiquement au runtime
        ),
        // Entraînement (un succès par thème)
        Achievement(
            id = TRAIN_PRINCIPES,
            titleRes = R.string.ach_train_principes_title,
            descriptionRes = R.string.ach_train_principes_desc,
            emoji = "⚖️",
            category = AchievementCategory.TRAINING
        ),
        Achievement(
            id = TRAIN_INSTITUTIONS,
            titleRes = R.string.ach_train_institutions_title,
            descriptionRes = R.string.ach_train_institutions_desc,
            emoji = "🏛️",
            category = AchievementCategory.TRAINING
        ),
        Achievement(
            id = TRAIN_DROITS,
            titleRes = R.string.ach_train_droits_title,
            descriptionRes = R.string.ach_train_droits_desc,
            emoji = "📜",
            category = AchievementCategory.TRAINING
        ),
        Achievement(
            id = TRAIN_HISTOIRE,
            titleRes = R.string.ach_train_histoire_title,
            descriptionRes = R.string.ach_train_histoire_desc,
            emoji = "🗺️",
            category = AchievementCategory.TRAINING
        ),
        Achievement(
            id = TRAIN_SOCIETE,
            titleRes = R.string.ach_train_societe_title,
            descriptionRes = R.string.ach_train_societe_desc,
            emoji = "🤝",
            category = AchievementCategory.TRAINING
        ),
        Achievement(
            id = TRAIN_ALL,
            titleRes = R.string.ach_train_all_title,
            descriptionRes = R.string.ach_train_all_desc,
            emoji = "🎓",
            category = AchievementCategory.TRAINING,
            rarity = AchievementRarity.RARE,
            target = 5
        ),
        // Fiches officielles (consultation hors-ligne)
        Achievement(
            id = FICHE_FIRST_READ,
            titleRes = R.string.ach_fiche_first_read_title,
            descriptionRes = R.string.ach_fiche_first_read_desc,
            emoji = "📖",
            category = AchievementCategory.FICHES
        ),
        Achievement(
            id = FICHE_30_READ,
            titleRes = R.string.ach_fiche_30_read_title,
            descriptionRes = R.string.ach_fiche_30_read_desc,
            emoji = "📚",
            category = AchievementCategory.FICHES,
            target = 30
        ),
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

    private val byIdMap: Map<String, Achievement> = ALL.associateBy { it.id }

    fun byId(id: String): Achievement? = byIdMap[id]
}
