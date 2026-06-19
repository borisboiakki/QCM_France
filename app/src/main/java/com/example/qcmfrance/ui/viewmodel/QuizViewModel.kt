package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val repository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun startQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val questions = repository.drawStratifiedQuestions()
            _uiState.update {
                QuizUiState(
                    questions = questions,
                    isLoading = false
                )
            }
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
    }

    fun restartQuiz() {
        _uiState.value = QuizUiState()
        startQuiz()
    }

    private fun runTimer() {
        viewModelScope.launch {
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
