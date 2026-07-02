package com.example.qcmfrance.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R
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
    onResetExamCycle: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetExamCycleDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_dialog_reset_training_title)) },
            text = { Text(stringResource(R.string.settings_dialog_reset_training_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetTraining()
                }) {
                    Text(stringResource(R.string.settings_dialog_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showResetExamCycleDialog) {
        AlertDialog(
            onDismissRequest = { showResetExamCycleDialog = false },
            title = { Text(stringResource(R.string.settings_dialog_reset_cycle_title)) },
            text = { Text(stringResource(R.string.settings_dialog_reset_cycle_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetExamCycleDialog = false
                    onResetExamCycle()
                }) {
                    Text(stringResource(R.string.settings_dialog_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetExamCycleDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    description = stringResource(R.string.settings_theme_system_desc),
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onSelect = { onThemeChange(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    description = stringResource(R.string.settings_theme_light_desc),
                    selected = currentTheme == ThemeMode.LIGHT,
                    onSelect = { onThemeChange(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    description = stringResource(R.string.settings_theme_dark_desc),
                    selected = currentTheme == ThemeMode.DARK,
                    onSelect = { onThemeChange(ThemeMode.DARK) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_text_size),
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
                Text(stringResource(R.string.settings_text_size_small), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.settings_text_size_medium), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.settings_text_size_large), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_sound),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Modifier.toggleable sur la ligne + Switch sans onCheckedChange : une seule
            // cible de focus pour TalkBack, annoncée comme interrupteur.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = soundEnabled,
                        role = Role.Switch,
                        onValueChange = onSoundChange
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_sound_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_sound_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_training),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_training_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_reset_training))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_exam),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_exam_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showResetExamCycleDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_reset_cycle))
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    // Modifier.selectable sur la ligne + RadioButton sans onClick : une seule cible
    // de focus par option pour TalkBack, avec le rôle « bouton radio » annoncé.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
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
