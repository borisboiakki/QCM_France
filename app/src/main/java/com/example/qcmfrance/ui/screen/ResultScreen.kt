package com.example.qcmfrance.ui.screen

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R
import com.example.qcmfrance.data.ExamConstants
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.ui.theme.FailureRed
import com.example.qcmfrance.ui.theme.SuccessGreen
import com.example.qcmfrance.ui.utils.ResultExporter
import com.example.qcmfrance.ui.viewmodel.QuizUiState

@Composable
fun ResultScreen(uiState: QuizUiState, soundEnabled: Boolean, onRestart: () -> Unit) {
    val context = LocalContext.current

    // Musique de fin d'examen : La Marseillaise si réussi, marche funèbre si échoué.
    // Jouée une seule fois par résultat (rememberSaveable : pas de relecture après une
    // rotation), uniquement si le son est activé. Libérée quand on quitte l'écran.
    var musicPlayed by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(soundEnabled, uiState.passed) {
        val player: MediaPlayer? = if (soundEnabled && !musicPlayed) {
            musicPlayed = true
            val resId = if (uiState.passed) R.raw.marseillaise else R.raw.marche_funebre
            runCatching { MediaPlayer.create(context, resId)?.apply { start() } }.getOrNull()
        } else {
            null
        }
        onDispose {
            player?.let { p ->
                runCatching { if (p.isPlaying) p.stop() }
                p.release()
            }
        }
    }

    val passColor = if (uiState.passed) SuccessGreen else FailureRed
    val passLabel = stringResource(if (uiState.passed) R.string.result_passed else R.string.result_failed)
    val durationSeconds = ExamConstants.EXAM_DURATION_SECONDS - uiState.remainingSeconds
    val durationStr = "%02d:%02d".format(durationSeconds / 60, durationSeconds % 60)

    var showOnlyWrong by remember { mutableStateOf(false) }
    val wrongQuestions = uiState.questions.filter { q -> uiState.answers[q.id] != q.correctAnswer }
    val displayedQuestions = if (showOnlyWrong) wrongQuestions else uiState.questions

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = passColor.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = passLabel,
                            style = MaterialTheme.typography.displaySmall,
                            color = passColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.result_score, uiState.score, uiState.questions.size),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.result_time_used, durationStr),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.timerExpired) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.result_timer_expired),
                                style = MaterialTheme.typography.bodySmall,
                                color = FailureRed
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.result_details),
                        style = MaterialTheme.typography.titleMedium
                    )
                    FilterChip(
                        selected = showOnlyWrong,
                        onClick = { showOnlyWrong = !showOnlyWrong },
                        label = { Text(stringResource(R.string.result_errors_chip, wrongQuestions.size)) }
                    )
                }
                HorizontalDivider()
            }

            if (displayedQuestions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.result_no_errors),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                itemsIndexed(displayedQuestions) { index, question ->
                    QuestionResultItem(
                        index = if (showOnlyWrong) uiState.questions.indexOf(question) + 1 else index + 1,
                        question = question,
                        givenAnswer = uiState.answers[question.id]
                    )
                }
            }

            item {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = stringResource(R.string.result_restart), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { ResultExporter.shareFullResult(context, uiState) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = stringResource(R.string.result_export), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuestionResultItem(index: Int, question: Question, givenAnswer: String?) {
    val correct = givenAnswer == question.correctAnswer
    val answerColor = when {
        givenAnswer == null -> MaterialTheme.colorScheme.onSurfaceVariant
        correct             -> SuccessGreen
        else                -> FailureRed
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$index. ${question.text}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.result_your_answer,
                        givenAnswer ?: stringResource(R.string.result_no_answer)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = answerColor
                )
                Text(
                    text = stringResource(R.string.result_correct_answer, question.correctAnswer),
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val correctText = when (question.correctAnswer) {
                "A" -> question.optionA
                "B" -> question.optionB
                "C" -> question.optionC
                else -> question.optionD
            }
            Text(
                text = correctText,
                style = MaterialTheme.typography.bodyMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold
            )
            if (question.source.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                // LinkAnnotation.Url remplace ClickableText (déprécié) : le lien est ouvert
                // par le gestionnaire d'URI par défaut et annoncé comme lien par TalkBack.
                val sourceLabel = stringResource(R.string.result_source_prefix)
                val linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                Text(
                    text = buildAnnotatedString {
                        append(sourceLabel)
                        withLink(LinkAnnotation.Url(question.source, linkStyles)) {
                            append(question.source)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
