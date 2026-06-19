package com.example.qcmfrance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.qcmfrance.ui.screen.HomeScreen
import com.example.qcmfrance.ui.screen.QuizScreen
import com.example.qcmfrance.ui.screen.ResultScreen
import com.example.qcmfrance.ui.viewmodel.QuizViewModel

private const val ROUTE_HOME   = "home"
private const val ROUTE_QUIZ   = "quiz"
private const val ROUTE_RESULT = "result"

@Composable
fun QcmNavGraph(navController: NavHostController = rememberNavController()) {
    val viewModel: QuizViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            HomeScreen(
                onStartExam = {
                    viewModel.startQuiz()
                    navController.navigate(ROUTE_QUIZ)
                }
            )
        }

        composable(ROUTE_QUIZ) {
            QuizScreen(
                uiState    = uiState,
                onSelect   = viewModel::selectAnswer,
                onNext     = viewModel::nextQuestion,
                onSubmit   = {
                    viewModel.submitQuiz()
                    navController.navigate(ROUTE_RESULT) {
                        popUpTo(ROUTE_QUIZ) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_RESULT) {
            ResultScreen(
                uiState   = uiState,
                onRestart = {
                    viewModel.restartQuiz()
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_RESULT) { inclusive = true }
                    }
                }
            )
        }
    }
}
