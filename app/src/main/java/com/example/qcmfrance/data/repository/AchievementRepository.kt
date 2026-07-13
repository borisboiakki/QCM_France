package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.AchievementDao
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.db.SeenQuestionDao
import com.example.qcmfrance.data.model.Achievement
import com.example.qcmfrance.data.model.AchievementRecord
import com.example.qcmfrance.data.model.AchievementState
import com.example.qcmfrance.data.model.Achievements
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
    private val questionDao: QuestionDao
) {
    // Buffer suffisant pour absorber plusieurs déblocages simultanés (ex. dernier thème + « Élève modèle »).
    private val _newlyUnlocked = MutableSharedFlow<Achievement>(extraBufferCapacity = 16)
    val newlyUnlocked: SharedFlow<Achievement> = _newlyUnlocked.asSharedFlow()

    /** Chaque succès du catalogue + son état persisté, pour la page « Succès ». */
    fun observe(): Flow<List<AchievementState>> =
        dao.observeAll().map { records ->
            val byId = records.associateBy { it.id }
            val totalQuestions = questionDao.count()
            Achievements.ALL.map { a ->
                val record = byId[a.id]
                val target =
                    if (a.id == Achievements.EXAM_ALL_SEEN && totalQuestions > 0) totalQuestions
                    else a.target
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

    /** Appelé à la soumission d'un examen. */
    suspend fun onExamCompleted(passed: Boolean, perfect: Boolean, questionIds: List<Int>) {
        unlock(Achievements.EXAM_FIRST_COMPLETED)
        if (passed) unlock(Achievements.EXAM_FIRST_PASSED)
        if (perfect) unlock(Achievements.EXAM_PERFECT)

        // « Tour complet » : accumule les ids vus, débloque quand toutes les questions ont été vues.
        seenDao.insertAll(questionIds.map { SeenQuestion(it) })
        val total = questionDao.count()
        if (total > 0) setProgress(Achievements.EXAM_ALL_SEEN, seenDao.count(), total)
    }

    /** Appelé quand un thème d'entraînement est terminé (ou déjà terminé à l'ouverture). */
    suspend fun onThemeCompleted(theme: String) {
        val id = Achievements.TRAINING_BY_THEME[theme] ?: return
        unlock(id)
        // « Élève modèle » : progression = nombre de thèmes terminés (une seule requête).
        val done = dao.countUnlockedIn(Achievements.TRAINING_BY_THEME.values.toList())
        setProgress(Achievements.TRAIN_ALL, done, Achievements.TRAINING_BY_THEME.size)
    }

    /** Réinitialise tous les succès et leur progression (depuis les Paramètres). */
    suspend fun resetAll() {
        dao.clear()
        seenDao.clear()
    }
}
