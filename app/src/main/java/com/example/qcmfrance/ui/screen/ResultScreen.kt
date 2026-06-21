package com.example.qcmfrance.ui.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.ui.utils.ResultExporter
import com.example.qcmfrance.ui.viewmodel.QuizUiState

private val GreenOk = Color(0xFF2E7D32)
private val RedFail = Color(0xFFC62828)

@Composable
fun ResultScreen(uiState: QuizUiState, onRestart: () -> Unit) {
    val context = LocalContext.current
    val passColor = if (uiState.passed) GreenOk else RedFail
    val passLabel = if (uiState.passed) "RÉUSSI" else "ÉCHOUÉ"
    val durationSeconds = 2700 - uiState.remainingSeconds
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
                            text = "${uiState.score} / ${uiState.questions.size}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Temps utilisé : $durationStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.timerExpired) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Temps écoulé — soumission automatique",
                                style = MaterialTheme.typography.bodySmall,
                                color = RedFail
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
                        text = "Détail des réponses",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FilterChip(
                        selected = showOnlyWrong,
                        onClick = { showOnlyWrong = !showOnlyWrong },
                        label = { Text("Erreurs (${wrongQuestions.size})") }
                    )
                }
                HorizontalDivider()
            }

            if (displayedQuestions.isEmpty()) {
                item {
                    Text(
                        text = "Aucune erreur — score parfait !",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreenOk,
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
                    Text(text = "Recommencer", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { ResultExporter.shareFullResult(context, uiState) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "Exporter les résultats", style = MaterialTheme.typography.titleMedium)
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
        correct             -> GreenOk
        else                -> RedFail
    }
    val uriHandler = LocalUriHandler.current

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
                    text = "Votre réponse : ${givenAnswer ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = answerColor
                )
                Text(
                    text = "Bonne réponse : ${question.correctAnswer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenOk
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
                style = MaterialTheme.typography.bodySmall,
                color = GreenOk,
                fontWeight = FontWeight.Bold
            )
            if (question.source.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val annotated = buildAnnotatedString {
                    append("Source : ")
                    pushStringAnnotation(tag = "URL", annotation = question.source)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(question.source)
                    }
                    pop()
                }
                androidx.compose.foundation.text.ClickableText(
                    text = annotated,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { offset ->
                        annotated.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }
                    }
                )
            }
        }
    }
}
