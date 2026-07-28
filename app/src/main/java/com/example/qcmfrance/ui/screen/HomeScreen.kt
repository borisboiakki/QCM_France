package com.example.qcmfrance.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R
import com.example.qcmfrance.data.model.ExamMode

// Couleurs du drapeau français
private val FlagBlue = Color(0xFF002395)
private val FlagRed = Color(0xFFED2939)
// Bleu plus clair que le bleu du drapeau, pour le bouton « Reprendre l'examen »
private val ResumeBlue = Color(0xFF4A73C8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    examMode: ExamMode,
    onExamModeChange: (ExamMode) -> Unit,
    onStartExam: () -> Unit,
    onResumeExam: () -> Unit,
    onStartTraining: () -> Unit,
    onShowHistory: () -> Unit,
    onShowAchievements: () -> Unit,
    onShowResources: () -> Unit,
    onShowSettings: () -> Unit,
    onShowHelp: () -> Unit,
    hasPausedQuiz: Boolean = false,
    pausedMode: ExamMode? = null
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.home_dialog_new_exam_title)) },
            text = { Text(stringResource(R.string.home_dialog_new_exam_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onStartExam()
                }) {
                    Text(stringResource(R.string.home_dialog_new_exam_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onShowResources) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.cd_resources)
                        )
                    }
                    IconButton(onClick = onShowHelp) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.cd_help)
                        )
                    }
                    IconButton(onClick = onShowSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Défilable : le sélecteur de QCM allonge l'écran, qui doit rester utilisable sur un petit
        // écran et avec la taille de texte « Grand ».
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = FlagBlue,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(FlagBlue))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(FlagRed))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Choix du QCM préparé. Les règles de l'examen (40 questions, 45 min, seuil 32/40)
            // sont identiques pour les trois et détaillées dans l'écran d'aide.
            Text(
                text = stringResource(R.string.home_mode_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExamMode.entries.forEach { mode ->
                    ExamModeRow(
                        mode = mode,
                        selected = mode == examMode,
                        onSelect = { onExamModeChange(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Couleurs du drapeau français : un bouton par couleur (bleu, blanc, rouge).
            Button(
                onClick = { if (hasPausedQuiz) showConfirmDialog = true else onStartExam() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlagBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.home_start_exam, stringResource(examMode.shortLabelRes)),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (hasPausedQuiz) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onResumeExam,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        // Bleu plus clair que le bleu du drapeau
                        containerColor = ResumeBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_resume_exam,
                            stringResource((pausedMode ?: examMode).shortLabelRes)
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.buttonColors(
                    // « Blanc » du drapeau : surface du thème pour rester lisible en mode sombre
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = stringResource(R.string.home_training), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onShowHistory,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlagRed,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(R.string.home_history), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onShowAchievements,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(text = stringResource(R.string.home_achievements), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ExamModeRow(mode: ExamMode, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
