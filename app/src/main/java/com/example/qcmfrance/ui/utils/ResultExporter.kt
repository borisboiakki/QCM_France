package com.example.qcmfrance.ui.utils

import android.content.Context
import android.content.Intent
import com.example.qcmfrance.R
import com.example.qcmfrance.data.ExamConstants
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.data.model.QuizResult
import com.example.qcmfrance.ui.viewmodel.QuizUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResultExporter {

    fun shareFullResult(context: Context, uiState: QuizUiState) {
        val durationSeconds = ExamConstants.EXAM_DURATION_SECONDS - uiState.remainingSeconds
        val durationStr = "%02d:%02d".format(durationSeconds / 60, durationSeconds % 60)
        val dateStr = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date())
        val passLabel = context.getString(
            if (uiState.passed) R.string.result_passed else R.string.result_failed
        )
        val percentage = if (uiState.questions.isNotEmpty()) uiState.score * 100 / uiState.questions.size else 0

        val sb = StringBuilder()
        sb.appendLine(context.getString(R.string.export_full_header, dateStr))
        sb.appendLine(
            context.getString(R.string.export_mode_line, context.getString(uiState.mode.labelRes))
        )
        sb.appendLine(
            context.getString(
                R.string.export_score_line,
                uiState.score, uiState.questions.size, percentage, passLabel
            )
        )
        sb.appendLine(context.getString(R.string.export_duration_line, durationStr))
        sb.appendLine()
        sb.appendLine(context.getString(R.string.export_details_label))
        sb.appendLine("─".repeat(40))

        uiState.questions.forEachIndexed { index, question ->
            val given = uiState.answers[question.id] ?: context.getString(R.string.result_no_answer)
            val correct = question.correctAnswer
            val mark = if (given == correct) "✓" else "✗"
            val correctText = when (correct) {
                "A" -> question.optionA
                "B" -> question.optionB
                "C" -> question.optionC
                else -> question.optionD
            }
            sb.appendLine("${index + 1}. [$mark] ${question.text}")
            sb.appendLine("   " + context.getString(R.string.export_your_answer, given))
            if (given != correct) {
                sb.appendLine("   " + context.getString(R.string.export_correct_answer, correct, correctText))
            }
        }

        share(context, sb.toString())
    }

    fun shareHistoryResult(context: Context, result: QuizResult) {
        val dateStr = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date(result.date))
        val durationStr = "%02d:%02d".format(result.durationSeconds / 60, result.durationSeconds % 60)
        val percentage = if (result.totalQuestions > 0) result.score * 100 / result.totalQuestions else 0
        val passLabel = context.getString(
            if (result.passed) R.string.result_passed else R.string.result_failed
        )

        val text = buildString {
            appendLine(context.getString(R.string.export_single_header, dateStr))
            appendLine(
                context.getString(
                    R.string.export_mode_line,
                    context.getString(ExamMode.fromCode(result.examMode).labelRes)
                )
            )
            appendLine(
                context.getString(
                    R.string.export_score_line,
                    result.score, result.totalQuestions, percentage, passLabel
                )
            )
            append(context.getString(R.string.export_duration_line, durationStr))
        }

        share(context, text)
    }

    private fun share(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.export_chooser_title))
        )
    }
}
