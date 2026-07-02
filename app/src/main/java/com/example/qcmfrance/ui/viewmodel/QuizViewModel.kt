package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.ExamConstants
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.QuizResult
import com.example.qcmfrance.data.repository.HistoryRepository
import com.example.qcmfrance.data.repository.PausedQuizRepository
import com.example.qcmfrance.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<Int, String> = emptyMap(),   // questionId → "A".."D"
    val isFinished: Boolean = false,
    val score: Int = 0,
    val passed: Boolean = false,
    val remainingSeconds: Int = ExamConstants.EXAM_DURATION_SECONDS,
    val timerExpired: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val historyRepository: HistoryRepository,
    private val pausedQuizRepository: PausedQuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startQuiz() {
        // Réinitialisation synchrone : un état isFinished=true resté d'un examen précédent
        // déclencherait la navigation immédiate vers l'écran résultat.
        timerJob?.cancel()
        _uiState.value = QuizUiState()
        viewModelScope.launch {
            pausedQuizRepository.clear()
            val questions = repository.drawStratifiedQuestions().map { it.withShuffledOptions() }
            _uiState.update {
                QuizUiState(
                    questions = questions,
                    isLoading = false
                )
            }
            runTimer()
        }
    }

    fun pauseQuiz() {
        timerJob?.cancel()
        val state = _uiState.value
        viewModelScope.launch {
            pausedQuizRepository.save(
                questions        = state.questions,
                answers          = state.answers,
                currentIndex     = state.currentIndex,
                remainingSeconds = state.remainingSeconds
            )
        }
    }

    fun resumeQuiz() {
        timerJob?.cancel()
        _uiState.value = QuizUiState()
        viewModelScope.launch {
            val saved = pausedQuizRepository.load() ?: return@launch
            pausedQuizRepository.clear()
            _uiState.value = QuizUiState(
                questions        = saved.questions,
                answers          = saved.answers,
                currentIndex     = saved.currentIndex,
                remainingSeconds = saved.remainingSeconds,
                isLoading        = false
            )
            runTimer()
        }
    }

    fun selectAnswer(letter: String) {
        val questionId = _uiState.value.questions.getOrNull(_uiState.value.currentIndex)?.id ?: return
        _uiState.update { state ->
            state.copy(answers = state.answers + (questionId to letter))
        }
    }

    fun nextQuestion() {
        _uiState.update { state ->
            if (state.currentIndex < state.questions.lastIndex)
                state.copy(currentIndex = state.currentIndex + 1)
            else state
        }
    }

    fun submitQuiz() {
        val state = _uiState.value
        if (state.isFinished) return   // évite un double envoi (double tap ou course avec le timer)
        val score = state.questions.count { q -> state.answers[q.id] == q.correctAnswer }
        val passed = score >= ExamConstants.PASS_THRESHOLD
        _uiState.update {
            it.copy(
                isFinished = true,
                score = score,
                passed = passed
            )
        }
        viewModelScope.launch {
            historyRepository.save(
                QuizResult(
                    date            = System.currentTimeMillis(),
                    score           = score,
                    passed          = passed,
                    durationSeconds = ExamConstants.EXAM_DURATION_SECONDS - state.remainingSeconds
                )
            )
        }
    }

    fun restartQuiz() {
        _uiState.value = QuizUiState()
    }

    /** Réinitialise le cycle de tirage de l'examen (depuis les Paramètres). */
    fun resetExamCycle() {
        viewModelScope.launch { repository.resetExamCycle() }
    }

    private fun runTimer() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && !_uiState.value.isFinished) {
                delay(1000L)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            if (_uiState.value.remainingSeconds == 0 && !_uiState.value.isFinished) {
                _uiState.update { it.copy(timerExpired = true) }
                submitQuiz()
            }
        }
    }
}
