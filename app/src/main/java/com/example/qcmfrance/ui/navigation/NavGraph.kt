package com.example.qcmfrance.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qcmfrance.data.model.Achievement
import com.example.qcmfrance.ui.components.AchievementUnlockedBanner
import com.example.qcmfrance.ui.screen.AboutScreen
import com.example.qcmfrance.ui.screen.AchievementsScreen
import com.example.qcmfrance.ui.screen.HelpScreen
import com.example.qcmfrance.ui.screen.HistoryScreen
import com.example.qcmfrance.ui.screen.HomeScreen
import com.example.qcmfrance.ui.screen.QuizScreen
import com.example.qcmfrance.ui.screen.ResourcesScreen
import com.example.qcmfrance.ui.screen.ResultScreen
import com.example.qcmfrance.ui.screen.SettingsScreen
import com.example.qcmfrance.ui.screen.TrainingScreen
import com.example.qcmfrance.ui.screen.TrainingThemesScreen
import com.example.qcmfrance.ui.viewmodel.AchievementsViewModel
import com.example.qcmfrance.ui.viewmodel.HistoryViewModel
import com.example.qcmfrance.ui.viewmodel.HomeViewModel
import com.example.qcmfrance.ui.viewmodel.QuizViewModel
import com.example.qcmfrance.ui.viewmodel.SettingsViewModel
import com.example.qcmfrance.ui.viewmodel.TrainingViewModel

private const val ROUTE_HOME            = "home"
private const val ROUTE_QUIZ            = "quiz"
private const val ROUTE_RESULT          = "result"
private const val ROUTE_HISTORY         = "history"
private const val ROUTE_SETTINGS        = "settings"
private const val ROUTE_HELP            = "help"
private const val ROUTE_RESOURCES       = "resources"
private const val ROUTE_TRAINING_THEMES = "training_themes"
private const val ROUTE_TRAINING        = "training"
private const val ROUTE_ABOUT           = "about"
private const val ROUTE_ACHIEVEMENTS    = "achievements"

@Composable
fun QcmNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: QuizViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val soundEnabled by settingsViewModel.soundEnabled.collectAsStateWithLifecycle()

    val trainingViewModel: TrainingViewModel = hiltViewModel()

    // Popup global de déblocage : collecté au-dessus de la navigation pour s'afficher quel que
    // soit l'écran courant. File d'attente si plusieurs succès tombent en même temps.
    val achievementsViewModel: AchievementsViewModel = hiltViewModel()
    val bannerQueue = remember { mutableStateListOf<Achievement>() }
    LaunchedEffect(Unit) {
        achievementsViewModel.newlyUnlocked.collect { bannerQueue.add(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            val hasPausedQuiz by homeViewModel.hasPausedQuiz.collectAsStateWithLifecycle()
            HomeScreen(
                onStartExam        = { viewModel.startQuiz(); navController.navigate(ROUTE_QUIZ) },
                onResumeExam       = { viewModel.resumeQuiz(); navController.navigate(ROUTE_QUIZ) },
                onStartTraining    = { navController.navigate(ROUTE_TRAINING_THEMES) },
                onShowHistory      = { navController.navigate(ROUTE_HISTORY) },
                onShowAchievements = { navController.navigate(ROUTE_ACHIEVEMENTS) },
                onShowResources    = { navController.navigate(ROUTE_RESOURCES) },
                onShowSettings     = { navController.navigate(ROUTE_SETTINGS) },
                onShowHelp         = { navController.navigate(ROUTE_HELP) },
                hasPausedQuiz      = hasPausedQuiz
            )
        }

        composable(ROUTE_TRAINING_THEMES) {
            val themes by trainingViewModel.themeProgress.collectAsStateWithLifecycle()
            TrainingThemesScreen(
                themes = themes,
                onSelectTheme = { theme ->
                    trainingViewModel.startTheme(theme)
                    navController.navigate(ROUTE_TRAINING)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_TRAINING) {
            val trainingState by trainingViewModel.uiState.collectAsStateWithLifecycle()
            TrainingScreen(
                uiState    = trainingState,
                onSelect   = trainingViewModel::selectAnswer,
                onConfirm  = trainingViewModel::confirmAnswer,
                onNext     = trainingViewModel::next,
                onPrevious = trainingViewModel::previous,
                onRestart  = trainingViewModel::restartTheme,
                onBack     = { navController.popBackStack(ROUTE_TRAINING_THEMES, inclusive = false) }
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
                onSubmit     = viewModel::submitQuiz,
                onPause      = {
                    viewModel.pauseQuiz()
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                },
                onAutoSave   = viewModel::saveSnapshot
            )
        }

        composable(ROUTE_RESULT) {
            ResultScreen(
                uiState      = uiState,
                soundEnabled = soundEnabled,
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
            val textSizeMode by settingsViewModel.textSizeMode.collectAsStateWithLifecycle()
            SettingsScreen(
                currentTheme         = themeMode,
                onThemeChange        = settingsViewModel::setThemeMode,
                soundEnabled         = soundEnabled,
                onSoundChange        = settingsViewModel::setSoundEnabled,
                textSizeMode         = textSizeMode,
                onTextSizeModeChange = settingsViewModel::setTextSizeMode,
                onResetTraining      = trainingViewModel::resetTraining,
                onResetExamCycle     = viewModel::resetExamCycle,
                onResetAchievements  = achievementsViewModel::resetAchievements,
                onShowAbout          = { navController.navigate(ROUTE_ABOUT) },
                onBack               = { navController.popBackStack() }
            )
        }

        composable(ROUTE_HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_RESOURCES) {
            ResourcesScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_ACHIEVEMENTS) {
            val states by achievementsViewModel.achievements.collectAsStateWithLifecycle()
            AchievementsScreen(
                states = states,
                onBack = { navController.popBackStack() }
            )
        }
    }

        // Overlay du popup de déblocage, au-dessus de tous les écrans.
        AchievementUnlockedBanner(
            achievement = bannerQueue.firstOrNull(),
            onDismiss = { if (bannerQueue.isNotEmpty()) bannerQueue.removeAt(0) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
