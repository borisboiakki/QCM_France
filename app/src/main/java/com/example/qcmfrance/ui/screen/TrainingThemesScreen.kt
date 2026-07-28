package com.example.qcmfrance.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R
import com.example.qcmfrance.data.model.ExamMode
import com.example.qcmfrance.ui.viewmodel.ModeProgress
import com.example.qcmfrance.ui.viewmodel.ThemeProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingThemesScreen(
    modes: List<ModeProgress>,
    ficheThemes: List<ThemeProgress>,
    onSelectTheme: (ExamMode, String) -> Unit,
    onSelectFicheTheme: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.themes_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.themes_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Une section par QCM : les mêmes 5 thèmes, avec une progression indépendante.
            modes.forEach { modeProgress ->
                item(key = "header_${modeProgress.mode.code}") {
                    SectionHeader(stringResource(modeProgress.mode.labelRes))
                }
                items(
                    modeProgress.themes,
                    key = { "${modeProgress.mode.code}_${it.theme}" }
                ) { theme ->
                    ThemeCard(
                        theme = theme,
                        onClick = { onSelectTheme(modeProgress.mode, theme.theme) }
                    )
                }
            }

            if (ficheThemes.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.training_official_sources_title))
                }
                item {
                    Text(
                        text = stringResource(R.string.training_official_sources_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(ficheThemes, key = { "fiche_${it.theme}" }) { theme ->
                    ThemeCard(theme = theme, onClick = { onSelectFicheTheme(theme.theme) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun ThemeCard(theme: ThemeProgress, onClick: () -> Unit) {
    val progress = if (theme.total > 0) theme.done.toFloat() / theme.total else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = theme.theme,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (theme.isComplete) stringResource(R.string.themes_done)
                           else stringResource(R.string.themes_progress, theme.done, theme.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (theme.isComplete) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
