package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.data.repository.PausedQuizRepository
import com.example.qcmfrance.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    pausedQuizRepository: PausedQuizRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hasPausedQuiz: StateFlow<Boolean> = pausedQuizRepository.observeHasPaused()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** QCM de l'examen en pause, pour libeller le bouton « Reprendre ». */
    val pausedMode: StateFlow<ExamMode?> = pausedQuizRepository.observePausedMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** QCM sélectionné sur l'accueil, persisté d'un lancement à l'autre. */
    val examMode: StateFlow<ExamMode> = settingsRepository.examMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExamMode.DEFAULT)

    fun setExamMode(mode: ExamMode) {
        viewModelScope.launch { settingsRepository.setExamMode(mode) }
    }
}
