package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.repository.PausedQuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pausedQuizRepository: PausedQuizRepository
) : ViewModel() {

    private val _hasPausedQuiz = MutableStateFlow(false)
    val hasPausedQuiz: StateFlow<Boolean> = _hasPausedQuiz.asStateFlow()

    init {
        viewModelScope.launch {
            _hasPausedQuiz.value = pausedQuizRepository.hasPaused()
        }
    }
}
