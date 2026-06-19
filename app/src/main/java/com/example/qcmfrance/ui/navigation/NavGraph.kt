package com.example.qcmfrance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qcmfrance.ui.screen.HistoryScreen
import com.example.qcmfrance.ui.screen.HomeScreen
import com.example.qcmfrance.ui.screen.QuizScreen
import com.example.qcmfrance.ui.screen.ResultScreen
import com.example.qcmfrance.ui.screen.SettingsScreen
import com.example.qcmfrance.ui.viewmodel.HistoryViewModel
import com.example.qcmfrance.ui.viewmodel.QuizViewModel
import com.example.qcmfrance.ui.viewmodel.SettingsViewModel

private const val ROUTE_HOME     = "home"
private const val ROUTE_QUIZ     = "quiz"
private const val ROUTE_RESULT   = "result"
private const val ROUTE_HISTORY  = "history"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun QcmNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: QuizViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val soundEnabled by settingsViewModel.soundEnabled.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onStartExam    = { viewModel.startQuiz(); navController.navigate(ROUTE_QUIZ) },
                onShowHistory  = { navController.navigate(ROUTE_HISTORY) },
                onShowSettings = { navController.navigate(ROUTE_SETTINGS) }
            )
        }

        composable(ROUTE_QUIZ) {
            LaunchedEffect(uiState.isFinished) {
                if (uiState.isFinished) {
                    navController.navigate(ROUTE_RESULT) {
                        popUpTo(ROUTE_QUIZ) { inclusive = true }
                    }
                }
            }
            QuizScreen(
                uiState      = uiState,
                soundEnabled = soundEnabled,
                onSelect     = viewModel::selectAnswer,
                onNext       = viewModel::nextQuestion,
                onSubmit     = viewModel::submitQuiz
            )
        }

        composable(ROUTE_RESULT) {
            ResultScreen(
                uiState   = uiState,
                onRestart = {
                    viewModel.restartQuiz()
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                }
            )
        }

        composable(ROUTE_HISTORY) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            val results by historyViewModel.results.collectAsStateWithLifecycle()
            HistoryScreen(
                results        = results,
                onBack         = { navController.popBackStack() },
                onClearHistory = historyViewModel::clearHistory
            )
        }

        composable(ROUTE_SETTINGS) {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                currentTheme  = themeMode,
                onThemeChange = settingsViewModel::setThemeMode,
                soundEnabled  = soundEnabled,
                onSoundChange = settingsViewModel::setSoundEnabled,
                onBack        = { navController.popBackStack() }
            )
        }
    }
}
