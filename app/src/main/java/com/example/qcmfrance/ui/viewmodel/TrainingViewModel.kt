package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class TrainingUiState(
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

    /** Totaux par thème (constants après amorçage) — calculés à l'ouverture de l'écran. */
    private val totals: StateFlow<Map<String, Int>> = flow {
        emit(repository.themes.associateWith { repository.totalForTheme(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Avancement de chaque thème pour l'écran de sélection. */
    val themeProgress: StateFlow<List<ThemeProgress>> =
        combine(repository.observeProgress(), totals) { progress, totalsByTheme ->
            repository.themes.map { theme ->
                ThemeProgress(
                    theme = theme,
                    done = (progress[theme] ?: 0).coerceAtMost(totalsByTheme[theme] ?: 0),
                    total = totalsByTheme[theme] ?: 0
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Charge un thème et reprend à la position sauvegardée. */
    fun startTheme(theme: String) {
        viewModelScope.launch {
            _uiState.value = TrainingUiState(theme = theme, isLoading = true)
            val questions = repository.questionsForTheme(theme).map { it.withShuffledOptions() }
            val savedIndex = repository.progressFor(theme)
            val alreadyDone = questions.isNotEmpty() && savedIndex >= questions.size
            _uiState.value = TrainingUiState(
                theme = theme,
                questions = questions,
                currentIndex = savedIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
                isLoading = false,
                isFinished = questions.isEmpty() || alreadyDone
            )
            // Rattrapage : un thème déjà terminé avant l'ajout des succès débloque le sien à l'ouverture.
            if (alreadyDone) achievementRepository.onThemeCompleted(theme)
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
                repository.saveProgress(state.theme, state.questions.size)
                achievementRepository.onThemeCompleted(state.theme)
            }
        } else {
            _uiState.update {
                it.copy(currentIndex = nextIndex, selectedAnswer = null, revealed = false)
            }
            viewModelScope.launch { repository.saveProgress(state.theme, nextIndex) }
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
        val theme = _uiState.value.theme
        viewModelScope.launch {
            repository.saveProgress(theme, 0)
            startTheme(theme)
        }
    }

    /** Réinitialise l'avancement de tous les thèmes (depuis les Paramètres). */
    fun resetTraining() {
        viewModelScope.launch { repository.resetAll() }
    }
}
