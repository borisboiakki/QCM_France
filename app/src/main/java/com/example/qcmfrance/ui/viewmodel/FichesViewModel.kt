package com.example.qcmfrance.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qcmfrance.data.model.FicheTheme
import com.example.qcmfrance.data.repository.FichesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alimente le navigateur hors-ligne des fiches thématiques. Charge une fois la liste des thèmes
 * (avec leurs fiches) depuis [FichesRepository] ; les écrans y piochent thèmes / fiches / détail.
 */
@HiltViewModel
class FichesViewModel @Inject constructor(
    private val repository: FichesRepository
) : ViewModel() {

    private val _themes = MutableStateFlow<List<FicheTheme>>(emptyList())
    val themes: StateFlow<List<FicheTheme>> = _themes.asStateFlow()

    init {
        viewModelScope.launch {
            _themes.value = repository.themes()
        }
    }
}
