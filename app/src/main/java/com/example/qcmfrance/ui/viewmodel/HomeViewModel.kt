package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.repository.PausedQuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    pausedQuizRepository: PausedQuizRepository
) : ViewModel() {

    val hasPausedQuiz: StateFlow<Boolean> = pausedQuizRepository.observeHasPaused()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
