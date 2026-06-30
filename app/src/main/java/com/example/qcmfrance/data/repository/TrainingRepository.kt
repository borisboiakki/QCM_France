package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.db.TrainingProgressDao
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.TrainingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Couche d'accès au mode « S'entraîner » : questions par thème (ordre stable) + persistance
 * de l'avancement par thème dans la table `training_progress`.
 */
@Singleton
class TrainingRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val progressDao: TrainingProgressDao,
    private val questionRepository: QuestionRepository
) {
    /** Les 5 thèmes officiels, dans l'ordre. */
    val themes: List<String> get() = questionRepository.themes

    /** Toutes les questions d'un thème, dans un ordre fixe (par id), après amorçage de la base. */
    suspend fun questionsForTheme(theme: String): List<Question> {
        questionRepository.seedIfNeeded()
        return questionDao.getAllByTheme(theme)
    }

    suspend fun totalForTheme(theme: String): Int {
        questionRepository.seedIfNeeded()
        return questionDao.countByTheme(theme)
    }

    /** Index de reprise sauvegardé pour un thème (0 si jamais commencé). */
    suspend fun progressFor(theme: String): Int = progressDao.get(theme)?.currentIndex ?: 0

    suspend fun saveProgress(theme: String, index: Int) {
        progressDao.save(TrainingProgress(theme = theme, currentIndex = index))
    }

    /** Avancement de tous les thèmes : theme -> index de reprise. */
    fun observeProgress(): Flow<Map<String, Int>> =
        progressDao.observeAll().map { list -> list.associate { it.theme to it.currentIndex } }

    /** Réinitialise l'avancement de tous les thèmes. */
    suspend fun resetAll() = progressDao.clear()
}
