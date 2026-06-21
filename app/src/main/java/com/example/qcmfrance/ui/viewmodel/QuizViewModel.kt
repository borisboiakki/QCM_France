package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val remainingSeconds: Int = 2700,             // 45 min
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
        viewModelScope.launch {
            pausedQuizRepository.clear()
            _uiState.update { it.copy(isLoading = true) }
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
        val score = state.questions.count { q -> state.answers[q.id] == q.correctAnswer }
        _uiState.update {
            it.copy(
                isFinished = true,
                score = score,
                passed = score >= 32
            )
        }
        viewModelScope.launch {
            historyRepository.save(
                QuizResult(
                    date            = System.currentTimeMillis(),
                    score           = score,
                    passed          = score >= 32,
                    durationSeconds = 2700 - state.remainingSeconds
                )
            )
        }
    }

    fun restartQuiz() {
        _uiState.value = QuizUiState()
    }

    private fun Question.withShuffledOptions(): Question {
        val letters = listOf("A", "B", "C", "D")
        val originals = listOf(optionA, optionB, optionC, optionD)
        val shuffledIndices = (0..3).shuffled()
        val newOptions = shuffledIndices.map { originals[it] }
        val origCorrectIdx = letters.indexOf(correctAnswer)
        val newCorrectIdx = shuffledIndices.indexOf(origCorrectIdx)
        return copy(
            optionA = newOptions[0],
            optionB = newOptions[1],
            optionC = newOptions[2],
            optionD = newOptions[3],
            correctAnswer = letters[newCorrectIdx]
        )
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
