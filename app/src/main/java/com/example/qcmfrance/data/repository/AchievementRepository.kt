package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.AchievementDao
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.db.SeenQuestionDao
import com.example.qcmfrance.data.model.Achievement
import com.example.qcmfrance.data.model.AchievementRecord
import com.example.qcmfrance.data.model.AchievementState
import com.example.qcmfrance.data.model.Achievements
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.data.model.SeenQuestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moteur de déblocage des succès. Point d'entrée unique, idempotent, alimenté depuis les flux
 * métier (fin d'examen, fin de thème d'entraînement). Émet chaque nouveau succès sur
 * [newlyUnlocked] pour l'affichage du popup, et expose [observe] pour la page « Succès ».
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val dao: AchievementDao,
    private val seenDao: SeenQuestionDao,
    private val questionDao: QuestionDao,
    private val fichesRepository: FichesRepository
) {
    // Buffer suffisant pour absorber plusieurs déblocages simultanés (ex. dernier thème + « Élève modèle »).
    private val _newlyUnlocked = MutableSharedFlow<Achievement>(extraBufferCapacity = 16)
    val newlyUnlocked: SharedFlow<Achievement> = _newlyUnlocked.asSharedFlow()

    /** Chaque succès du catalogue + son état persisté, pour la page « Succès ». */
    fun observe(): Flow<List<AchievementState>> =
        dao.observeAll().map { records ->
            val byId = records.associateBy { it.id }
            // Cibles résolues au runtime : nombre de questions de chaque QCM (« Tour complet ») et
            // nombre total de fiches (« Bibliothèque complète »).
            val dynamicTargets = buildMap {
                Achievements.EXAM_ALL_SEEN_BY_MODE.forEach { (mode, id) ->
                    val total = questionDao.countForMode(mode.code)
                    if (total > 0) put(id, total)
                }
                val totalFiches = fichesRepository.totalFichesCount()
                if (totalFiches > 0) put(Achievements.FICHE_ALL_READ, totalFiches)
            }
            Achievements.ALL.map { a ->
                val record = byId[a.id]
                val target = dynamicTargets[a.id] ?: a.target
                AchievementState(
                    achievement = a,
                    unlockedAt = record?.unlockedAt,
                    progress = (record?.progress ?: 0).coerceIn(0, target),
                    target = target
                )
            }
        }

    /** Débloque un succès « tout ou rien ». Idempotent : ne ré-émet pas un succès déjà débloqué. */
    suspend fun unlock(id: String) {
        val existing = dao.get(id)
        if (existing?.unlockedAt != null) return
        dao.upsert(AchievementRecord(id, unlockedAt = System.currentTimeMillis(), progress = existing?.progress ?: 0))
        Achievements.byId(id)?.let { _newlyUnlocked.tryEmit(it) }
    }

    /**
     * Met à jour la progression d'un succès à cible et le débloque au passage de [target].
     * Idempotent : une fois débloqué, seule la progression (jamais décroissante) est conservée.
     */
    private suspend fun setProgress(id: String, current: Int, target: Int) {
        val existing = dao.get(id)
        if (existing?.unlockedAt != null) {
            if (current > existing.progress) dao.upsert(existing.copy(progress = current))
            return
        }
        val reached = target > 0 && current >= target
        dao.upsert(AchievementRecord(id, unlockedAt = if (reached) System.currentTimeMillis() else null, progress = current))
        if (reached) Achievements.byId(id)?.let { _newlyUnlocked.tryEmit(it) }
    }

    /**
     * Appelé à la soumission d'un examen. Les succès « premier examen », « reçu » et « sans-faute »
     * sont communs aux trois QCM ; « Tour complet » est propre au QCM passé.
     */
    suspend fun onExamCompleted(
        mode: ExamMode,
        passed: Boolean,
        perfect: Boolean,
        questionIds: List<Int>
    ) {
        unlock(Achievements.EXAM_FIRST_COMPLETED)
        if (passed) unlock(Achievements.EXAM_FIRST_PASSED)
        if (perfect) unlock(Achievements.EXAM_PERFECT)

        // « Tour complet » : accumule les ids vus (tous QCM confondus, les ids étant uniques), puis
        // ne compte pour ce QCM que les questions qui lui sont tirables (les siennes + les mises en
        // situation, communes). Un examen CR ne fait donc pas progresser le succès naturalisation.
        seenDao.insertAll(questionIds.map { SeenQuestion(it) })
        val achievementId = Achievements.EXAM_ALL_SEEN_BY_MODE[mode] ?: return
        val total = questionDao.countForMode(mode.code)
        if (total > 0) setProgress(achievementId, questionDao.countSeenForMode(mode.code), total)
    }

    /** Appelé quand un thème d'entraînement est terminé (ou déjà terminé à l'ouverture). */
    suspend fun onThemeCompleted(mode: ExamMode, theme: String) {
        val id = Achievements.TRAINING_BY_KEY[mode.trainingKey(theme)] ?: return
        unlock(id)
        // « Tous les thèmes » du QCM : progression = nombre de ses thèmes terminés (une requête).
        val themeIds = Achievements.TRAINING_THEME_IDS_BY_MODE[mode] ?: return
        val allId = Achievements.TRAINING_ALL_BY_MODE[mode] ?: return
        setProgress(allId, dao.countUnlockedIn(themeIds), themeIds.size)
    }

    /**
     * Appelé à l'ouverture d'une fiche (une fiche déjà marquée lue n'est comptée qu'une fois grâce
     * à `read_fiche`). Débloque « première fiche », progresse vers « 30 fiches » et « toutes les
     * fiches » (cible = nombre total de fiches).
     */
    suspend fun onFicheRead(readCount: Int, totalFiches: Int) {
        unlock(Achievements.FICHE_FIRST_READ)
        setProgress(Achievements.FICHE_30_READ, readCount, 30)
        if (totalFiches > 0) setProgress(Achievements.FICHE_ALL_READ, readCount, totalFiches)
    }

    /** Réinitialise tous les succès et leur progression (depuis les Paramètres). */
    suspend fun resetAll() {
        dao.clear()
        seenDao.clear()
    }
}
