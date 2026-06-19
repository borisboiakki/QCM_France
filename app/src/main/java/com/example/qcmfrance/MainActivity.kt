package com.example.qcmfrance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.qcmfrance.data.repository.SettingsRepository
import com.example.qcmfrance.data.repository.ThemeMode
import com.example.qcmfrance.ui.navigation.QcmNavGraph
import com.example.qcmfrance.ui.theme.QcmFranceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            QcmFranceTheme(themeMode = themeMode) {
                QcmNavGraph()
            }
        }
    }
}
