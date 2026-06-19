package com.example.qcmfrance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.qcmfrance.ui.navigation.QcmNavGraph
import com.example.qcmfrance.ui.theme.QcmFranceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QcmFranceTheme {
                QcmNavGraph()
            }
        }
    }
}
