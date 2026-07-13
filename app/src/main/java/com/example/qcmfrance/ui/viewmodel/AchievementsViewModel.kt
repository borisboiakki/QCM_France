package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.Achievement
import com.example.qcmfrance.data.model.AchievementState
import com.example.qcmfrance.data.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val repository: AchievementRepository
) : ViewModel() {

    /** Tous les succès + leur état, pour la page « Succès ». */
    val achievements: StateFlow<List<AchievementState>> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Émet chaque succès nouvellement débloqué (pour le popup global). */
    val newlyUnlocked: SharedFlow<Achievement> = repository.newlyUnlocked

    fun resetAchievements() {
        viewModelScope.launch { repository.resetAll() }
    }
}
