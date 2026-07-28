package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.db.TrainingProgressDao
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.TrainingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Couche d'accès au mode « S'entraîner » : questions par QCM et par thème (ordre stable) +
 * persistance de l'avancement dans la table `training_progress`.
 *
 * Chaque QCM a sa propre progression : la clé de la table est [ExamMode.trainingKey], qui conserve
 * le nom nu du thème pour la naturalisation (compatibilité avec les installations existantes) et le
 * préfixe par le code du QCM pour les autres.
 */
@Singleton
class TrainingRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val progressDao: TrainingProgressDao,
    private val questionRepository: QuestionRepository
) {
    /** Les 5 thèmes officiels, dans l'ordre. Communs aux trois QCM. */
    val themes: List<String> get() = questionRepository.themes

    /**
     * Toutes les questions d'un thème pour un QCM, dans un ordre fixe (par id), après amorçage de
     * la base. Les mises en situation, communes aux trois QCM, sont incluses.
     */
    suspend fun questionsForTheme(mode: ExamMode, theme: String): List<Question> {
        questionRepository.seedIfNeeded()
        return questionDao.getAllByTheme(theme, mode.code)
    }

    /**
     * Total d'items d'entraînement d'un thème : chaque question compte pour 1 + son nombre de
     * jeux de réponses alternatifs (`variants`), puisque l'entraînement déroule tous les jeux.
     * Doit rester cohérent avec l'expansion faite par TrainingViewModel.startTheme().
     */
    suspend fun totalForTheme(mode: ExamMode, theme: String): Int {
        questionRepository.seedIfNeeded()
        return questionDao.getAllByTheme(theme, mode.code).sumOf { 1 + it.variants.size }
    }

    /** Index de reprise sauvegardé pour un thème d'un QCM (0 si jamais commencé). */
    suspend fun progressFor(mode: ExamMode, theme: String): Int =
        progressDao.get(mode.trainingKey(theme))?.currentIndex ?: 0

    suspend fun saveProgress(mode: ExamMode, theme: String, index: Int) {
        progressDao.save(TrainingProgress(theme = mode.trainingKey(theme), currentIndex = index))
    }

    /** Avancement de tous les thèmes de tous les QCM : clé d'entraînement -> index de reprise. */
    fun observeProgress(): Flow<Map<String, Int>> =
        progressDao.observeAll().map { list -> list.associate { it.theme to it.currentIndex } }

    /** Réinitialise l'avancement de tous les thèmes, tous QCM confondus. */
    suspend fun resetAll() = progressDao.clear()
}
