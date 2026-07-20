package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.FicheTheme
import com.example.qcmfrance.data.repository.AchievementRepository
import com.example.qcmfrance.data.repository.FichesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alimente le navigateur hors-ligne des fiches thématiques. Charge une fois la liste des thèmes
 * (avec leurs fiches) depuis [FichesRepository] ; les écrans y piochent thèmes / fiches / détail.
 *
 * Gère aussi le **suivi de lecture** des fiches : progression par thème (X/total) pour l'écran
 * « S'entraîner » et déclenchement des succès « Fiches officielles ».
 */
@HiltViewModel
class FichesViewModel @Inject constructor(
    private val repository: FichesRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _themes = MutableStateFlow<List<FicheTheme>>(emptyList())
    val themes: StateFlow<List<FicheTheme>> = _themes.asStateFlow()

    /** Avancement de lecture par thème de fiches, pour l'écran « S'entraîner » (mêmes barres X/total). */
    val ficheThemeProgress: StateFlow<List<ThemeProgress>> =
        combine(_themes, repository.observeReadIds()) { themes, readIds ->
            val read = readIds.toHashSet()
            themes.map { t ->
                ThemeProgress(
                    theme = t.theme,
                    done = t.fiches.count { it.id in read },
                    total = t.fiches.size
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _themes.value = repository.themes()
        }
    }

    /** Marque une fiche comme consultée (à l'ouverture) et met à jour les succès fiches. */
    fun markRead(ficheId: String) {
        viewModelScope.launch {
            repository.markRead(ficheId)
            achievementRepository.onFicheRead(repository.readCount(), repository.totalFichesCount())
        }
    }

    /** Réinitialise le suivi de lecture des fiches (bouton « Réinitialiser la progression »). */
    fun resetReadFiches() {
        viewModelScope.launch { repository.clearRead() }
    }
}
