package com.example.qcmfrance.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.ui.viewmodel.TrainingUiState

private val CorrectGreen = Color(0xFF2E7D32)
private val WrongRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.theme,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.isFinished -> {
                ThemeCompleted(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    total = uiState.questions.size,
                    onRestart = onRestart,
                    onBack = onBack
                )
            }

            else -> {
                val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return@Scaffold
                val uriHandler = LocalUriHandler.current
                val progress = (uiState.currentIndex + 1).toFloat() / uiState.questions.size

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Question ${uiState.currentIndex + 1} / ${uiState.questions.size}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val options = listOf(
                        "A" to question.optionA,
                        "B" to question.optionB,
                        "C" to question.optionC,
                        "D" to question.optionD
                    )

                    options.forEach { (letter, text) ->
                        TrainingOptionRow(
                            letter = letter,
                            text = text,
                            selected = uiState.selectedAnswer == letter,
                            revealed = uiState.revealed,
                            isCorrectOption = question.correctAnswer == letter,
                            onClick = { if (!uiState.revealed) onSelect(letter) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.revealed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBlock(
                            isCorrect = uiState.selectedAnswer == question.correctAnswer,
                            explanation = question.explanation,
                            source = question.source,
                            onOpenSource = { uriHandler.openUri(question.source) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isLast = uiState.currentIndex == uiState.questions.lastIndex
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.currentIndex > 0) {
                            OutlinedButton(
                                onClick = onPrevious,
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Text("Précédent", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Button(
                            onClick = if (!uiState.revealed) onConfirm else onNext,
                            enabled = uiState.revealed || uiState.selectedAnswer != null,
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(
                                text = when {
                                    !uiState.revealed -> "Confirmer"
                                    isLast -> "Terminer"
                                    else -> "Suivant"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TrainingOptionRow(
    letter: String,
    text: String,
    selected: Boolean,
    revealed: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit
) {
    // Couleur d'accent après révélation : vert pour la bonne réponse,
    // rouge pour une mauvaise réponse sélectionnée.
    val accent: Color? = when {
        !revealed -> null
        isCorrectOption -> CorrectGreen
        selected -> WrongRed
        else -> null
    }
    val border = accent?.let { BorderStroke(2.dp, it) } ?: CardDefaults.outlinedCardBorder()

    OutlinedCard(
        onClick = onClick,
        enabled = !revealed,
        modifier = Modifier.fillMaxWidth(),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = !revealed
            )
            Text(
                text = "$letter.  $text",
                style = MaterialTheme.typography.bodyMedium,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            if (revealed && isCorrectOption) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Bonne réponse",
                    tint = CorrectGreen
                )
            } else if (revealed && selected) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Réponse incorrecte",
                    tint = WrongRed
                )
            }
        }
    }
}

@Composable
private fun FeedbackBlock(
    isCorrect: Boolean,
    explanation: String,
    source: String,
    onOpenSource: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isCorrect) "Bonne réponse !" else "Mauvaise réponse",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) CorrectGreen else WrongRed
            )

            if (explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (source.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenSource) {
                    Text("Voir la source")
                }
            }
        }
    }
}

@Composable
private fun ThemeCompleted(
    modifier: Modifier = Modifier,
    total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CorrectGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Thème terminé !",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vous avez parcouru les $total questions de ce thème.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Recommencer ce thème", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Retour aux thèmes")
        }
    }
}
