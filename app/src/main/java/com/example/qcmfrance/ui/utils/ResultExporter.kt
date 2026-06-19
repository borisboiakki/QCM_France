package com.example.qcmfrance.ui.utils

import android.content.Context
import android.content.Intent
import com.example.qcmfrance.data.model.QuizResult
import com.example.qcmfrance.ui.viewmodel.QuizUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResultExporter {

    fun shareFullResult(context: Context, uiState: QuizUiState) {
        val durationSeconds = 2700 - uiState.remainingSeconds
        val durationStr = "%02d:%02d".format(durationSeconds / 60, durationSeconds % 60)
        val dateStr = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date())
        val passLabel = if (uiState.passed) "RÉUSSI" else "ÉCHOUÉ"
        val percentage = if (uiState.questions.isNotEmpty()) uiState.score * 100 / uiState.questions.size else 0

        val sb = StringBuilder()
        sb.appendLine("QCM France — Résultats du $dateStr")
        sb.appendLine("Score : ${uiState.score}/${uiState.questions.size} ($percentage%) — $passLabel")
        sb.appendLine("Durée : $durationStr")
        sb.appendLine()
        sb.appendLine("Détail des réponses :")
        sb.appendLine("─".repeat(40))

        uiState.questions.forEachIndexed { index, question ->
            val given = uiState.answers[question.id] ?: "—"
            val correct = question.correctAnswer
            val mark = if (given == correct) "✓" else "✗"
            val correctText = when (correct) {
                "A" -> question.optionA
                "B" -> question.optionB
                "C" -> question.optionC
                else -> question.optionD
            }
            sb.appendLine("${index + 1}. [$mark] ${question.text}")
            sb.appendLine("   Ta réponse    : $given")
            if (given != correct) {
                sb.appendLine("   Bonne réponse : $correct — $correctText")
            }
        }

        share(context, sb.toString())
    }

    fun shareHistoryResult(context: Context, result: QuizResult) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date(result.date))
        val durationStr = "%02d:%02d".format(result.durationSeconds / 60, result.durationSeconds % 60)
        val percentage = if (result.totalQuestions > 0) result.score * 100 / result.totalQuestions else 0
        val passLabel = if (result.passed) "RÉUSSI" else "ÉCHOUÉ"

        val text = buildString {
            appendLine("QCM France — Résultat du $dateStr")
            appendLine("Score : ${result.score}/${result.totalQuestions} ($percentage%) — $passLabel")
            append("Durée : $durationStr")
        }

        share(context, text)
    }

    private fun share(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Résultats QCM France")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Exporter via…"))
    }
}
