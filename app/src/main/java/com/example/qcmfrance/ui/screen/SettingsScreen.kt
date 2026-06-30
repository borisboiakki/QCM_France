package com.example.qcmfrance.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.data.repository.TextSizeMode
import com.example.qcmfrance.data.repository.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    textSizeMode: TextSizeMode,
    onTextSizeModeChange: (TextSizeMode) -> Unit,
    onResetTraining: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Réinitialiser la progression ?") },
            text = { Text("Toute votre progression du mode entraînement (tous les thèmes) sera effacée. Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetTraining()
                }) {
                    Text("Réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Apparence",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            ThemeOption(
                label = "Système (par défaut)",
                description = "Suit le thème du téléphone",
                mode = ThemeMode.SYSTEM,
                selected = currentTheme == ThemeMode.SYSTEM,
                onSelect = { onThemeChange(ThemeMode.SYSTEM) }
            )
            ThemeOption(
                label = "Clair",
                description = "Fond blanc, texte sombre",
                mode = ThemeMode.LIGHT,
                selected = currentTheme == ThemeMode.LIGHT,
                onSelect = { onThemeChange(ThemeMode.LIGHT) }
            )
            ThemeOption(
                label = "Sombre",
                description = "Fond noir, texte clair",
                mode = ThemeMode.DARK,
                selected = currentTheme == ThemeMode.DARK,
                onSelect = { onThemeChange(ThemeMode.DARK) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Taille du texte",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            val sliderValue = when (textSizeMode) {
                TextSizeMode.SMALL  -> 0f
                TextSizeMode.MEDIUM -> 1f
                TextSizeMode.LARGE  -> 2f
            }
            Slider(
                value = sliderValue,
                onValueChange = { v ->
                    onTextSizeModeChange(
                        when (v.roundToInt()) {
                            0    -> TextSizeMode.SMALL
                            2    -> TextSizeMode.LARGE
                            else -> TextSizeMode.MEDIUM
                        }
                    )
                },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Petit", style = MaterialTheme.typography.bodySmall)
                Text("Moyen", style = MaterialTheme.typography.bodySmall)
                Text("Grand", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Son",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSoundChange(!soundEnabled) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Son de sélection", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Bip lors de la sélection d'une réponse",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = onSoundChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Entraînement",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Réinitialiser votre progression dans tous les thèmes du mode entraînement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Réinitialiser la progression")
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
