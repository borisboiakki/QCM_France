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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Couleurs du drapeau français
private val FlagBlue = Color(0xFF002395)
private val FlagRed = Color(0xFFED2939)
// Bleu plus clair que le bleu du drapeau, pour le bouton « Reprendre l'examen »
private val ResumeBlue = Color(0xFF4A73C8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartExam: () -> Unit,
    onResumeExam: () -> Unit,
    onStartTraining: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    onShowHelp: () -> Unit,
    hasPausedQuiz: Boolean = false
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Commencer un nouvel examen ?") },
            text = { Text("Un examen en pause sera définitivement perdu. Continuer ?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onStartExam()
                }) {
                    Text("Commencer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onShowHelp) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Aide"
                        )
                    }
                    IconButton(onClick = onShowSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "QCM France",
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
                text = "Préparation à l'examen civique de naturalisation",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuleRow(label = "Questions",          value = "40")
                    RuleRow(label = "Seuil de réussite",  value = "32 / 40 (80 %)")
                    RuleRow(label = "Durée maximale",     value = "45 minutes")
                    RuleRow(label = "Format",             value = "1 bonne réponse parmi 4")
                    RuleRow(label = "Correction",         value = "Uniquement à la fin")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Couleurs du drapeau français : un bouton par couleur (bleu, blanc, rouge).
            Button(
                onClick = { if (hasPausedQuiz) showConfirmDialog = true else onStartExam() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlagBlue,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Commencer l'examen", style = MaterialTheme.typography.titleMedium)
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
                    Text(text = "Reprendre l'examen", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.dp, FlagBlue),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = FlagBlue
                )
            ) {
                Text(text = "S'entraîner par thème", style = MaterialTheme.typography.titleMedium)
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
                Text(text = "Historique des examens", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RuleRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
