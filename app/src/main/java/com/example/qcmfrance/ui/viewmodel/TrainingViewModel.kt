package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.repository.AchievementRepository
import com.example.qcmfrance.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Avancement d'un thème pour l'écran de sélection. */
data class ThemeProgress(
    val theme: String,
    val done: Int,
    val total: Int
) {
    val isComplete: Boolean get() = total > 0 && done >= total
}

/** Avancement des 5 thèmes d'un QCM, pour une section de l'écran « S'entraîner ». */
data class ModeProgress(
    val mode: ExamMode,
    val themes: List<ThemeProgress>
)

data class TrainingUiState(
    val mode: ExamMode = ExamMode.DEFAULT,
    val theme: String = "",
    val questions: List<Question> = emptyList(),  // toutes les questions du thème, options mélangées
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,           // réponse choisie pour la question courante
    val revealed: Boolean = false,                // feedback affiché après sélection
    val isLoading: Boolean = true,
    val isFinished: Boolean = false               // fin du thème atteinte
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repository: TrainingRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    /**
     * Totaux par QCM et par thème (constants après amorçage), indexés par la clé d'entraînement —
     * calculés à l'ouverture de l'écran.
     */
    private val totals: StateFlow<Map<String, Int>> = flow {
        emit(
            ExamMode.entries.flatMap { mode ->
                repository.themes.map { theme ->
                    mode.trainingKey(theme) to repository.totalForTheme(mode, theme)
                }
            }.toMap()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Avancement de chaque thème de chaque QCM, pour les sections de l'écran de sélection. */
    val modeProgress: StateFlow<List<ModeProgress>> =
        combine(repository.observeProgress(), totals) { progress, totalsByKey ->
            ExamMode.entries.map { mode ->
                ModeProgress(
                    mode = mode,
                    themes = repository.themes.map { theme ->
                        val key = mode.trainingKey(theme)
                        val total = totalsByKey[key] ?: 0
                        ThemeProgress(
                            theme = theme,
                            done = (progress[key] ?: 0).coerceAtMost(total),
                            total = total
                        )
                    }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Charge un thème d'un QCM et reprend à la position sauvegardée. */
    fun startTheme(mode: ExamMode, theme: String) {
        viewModelScope.launch {
            _uiState.value = TrainingUiState(mode = mode, theme = theme, isLoading = true)
            // Contrairement à l'examen (un jeu tiré au hasard), l'entraînement déroule TOUS les
            // jeux de réponses d'une question à variantes, à la suite les uns des autres.
            val questions = repository.questionsForTheme(mode, theme)
                .flatMap { it.allAnswerSets() }
                .map { it.withShuffledOptions() }
            val savedIndex = repository.progressFor(mode, theme)
            val alreadyDone = questions.isNotEmpty() && savedIndex >= questions.size
            _uiState.value = TrainingUiState(
                mode = mode,
                theme = theme,
                questions = questions,
                currentIndex = savedIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
                isLoading = false,
                isFinished = questions.isEmpty() || alreadyDone
            )
            // Rattrapage : un thème déjà terminé avant l'ajout des succès débloque le sien à l'ouverture.
            if (alreadyDone) achievementRepository.onThemeCompleted(mode, theme)
        }
    }

    /** Sélectionne une réponse (modifiable tant qu'elle n'est pas confirmée). */
    fun selectAnswer(letter: String) {
        _uiState.update { state ->
            if (state.revealed) state
            else state.copy(selectedAnswer = letter)
        }
    }

    /** Confirme la réponse sélectionnée et révèle le feedback (bonne réponse, explication, source). */
    fun confirmAnswer() {
        _uiState.update { state ->
            if (state.revealed || state.selectedAnswer == null) state
            else state.copy(revealed = true)
        }
    }

    /** Passe à la question suivante (ou termine le thème) et persiste l'avancement. */
    fun next() {
        val state = _uiState.value
        if (!state.revealed) return
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            _uiState.update { it.copy(isFinished = true, selectedAnswer = null, revealed = false) }
            viewModelScope.launch {
                repository.saveProgress(state.mode, state.theme, state.questions.size)
                achievementRepository.onThemeCompleted(state.mode, state.theme)
            }
        } else {
            _uiState.update {
                it.copy(currentIndex = nextIndex, selectedAnswer = null, revealed = false)
            }
            viewModelScope.launch { repository.saveProgress(state.mode, state.theme, nextIndex) }
        }
    }

    /**
     * Revient à la question précédente et réinitialise la sélection.
     * N'écrit pas l'avancement : revoir une question ne doit pas faire régresser
     * la progression persistée (barre X/total et point de reprise).
     */
    fun previous() {
        val state = _uiState.value
        if (state.currentIndex <= 0 || state.isFinished) return
        _uiState.update { it.copy(currentIndex = state.currentIndex - 1, selectedAnswer = null, revealed = false) }
    }

    /** Rejoue un thème depuis le début. */
    fun restartTheme() {
        val mode = _uiState.value.mode
        val theme = _uiState.value.theme
        viewModelScope.launch {
            repository.saveProgress(mode, theme, 0)
            startTheme(mode, theme)
        }
    }

    /** Réinitialise l'avancement de tous les thèmes (depuis les Paramètres). */
    fun resetTraining() {
        viewModelScope.launch { repository.resetAll() }
    }
}
