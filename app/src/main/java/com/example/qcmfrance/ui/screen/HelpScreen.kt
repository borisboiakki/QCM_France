package com.example.qcmfrance.ui.screen

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R

private data class ExternalLink(@StringRes val labelRes: Int, val url: String)

private val OFFICIAL_LINKS = listOf(
    ExternalLink(
        labelRes = R.string.help_link_general,
        url = "https://formation-civique.interieur.gouv.fr/examen-civique/informations-g%C3%A9n%C3%A9rales-sur-lexamen-civique/"
    ),
    ExternalLink(
        labelRes = R.string.help_link_quiz,
        url = "https://www.ensemble-en-france.org/quiz-examen-civique-gratuit-debutant/"
    ),
    ExternalLink(
        labelRes = R.string.help_link_factsheet,
        url = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/lexamen-civique-pour-demande-de-naturalisation-ou-de-reintegration-dans-nationalite-francaise.html"
    ),
    ExternalLink(
        labelRes = R.string.help_link_citizen_booklet,
        url = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/livret-du-citoyen.html"
    ),
    ExternalLink(
        labelRes = R.string.help_link_charter,
        url = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/charte-des-droits-et-devoirs-du-citoyen-francais.html"
    ),
    ExternalLink(
        labelRes = R.string.help_link_declaration,
        url = "https://www.conseil-constitutionnel.fr/le-bloc-de-constitutionnalite/declaration-des-droits-de-l-homme-et-du-citoyen-de-1789"
    ),
    ExternalLink(
        labelRes = R.string.help_link_constitution,
        url = "https://www.legifrance.gouv.fr/loda/id/JORFTEXT000000571356/"
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HelpSectionTitle(stringResource(R.string.help_about_title))
                Text(
                    text = stringResource(R.string.help_about_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_rules_title))
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
                        HelpRuleRow(
                            stringResource(R.string.home_rule_questions_label),
                            stringResource(R.string.home_rule_questions_value)
                        )
                        HelpRuleRow(
                            stringResource(R.string.home_rule_pass_label),
                            stringResource(R.string.home_rule_pass_value)
                        )
                        HelpRuleRow(
                            stringResource(R.string.home_rule_duration_label),
                            stringResource(R.string.home_rule_duration_value)
                        )
                        HelpRuleRow(
                            stringResource(R.string.home_rule_format_label),
                            stringResource(R.string.home_rule_format_value)
                        )
                        HelpRuleRow(
                            stringResource(R.string.home_rule_results_label),
                            stringResource(R.string.home_rule_results_value)
                        )
                    }
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_themes_title))
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
                        HelpRuleRow(stringResource(R.string.help_theme_1), stringResource(R.string.help_theme_count, 6))
                        HelpRuleRow(stringResource(R.string.help_theme_2), stringResource(R.string.help_theme_count, 9))
                        HelpRuleRow(stringResource(R.string.help_theme_3), stringResource(R.string.help_theme_count, 6))
                        HelpRuleRow(stringResource(R.string.help_theme_4), stringResource(R.string.help_theme_count, 13))
                        HelpRuleRow(stringResource(R.string.help_theme_5), stringResource(R.string.help_theme_count, 6))
                    }
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_exam_title))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_timer_title),
                        stringResource(R.string.help_exam_timer_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_situation_title),
                        stringResource(R.string.help_exam_situation_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_rotation_title),
                        stringResource(R.string.help_exam_rotation_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_pause_title),
                        stringResource(R.string.help_exam_pause_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_history_title),
                        stringResource(R.string.help_exam_history_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_exam_export_title),
                        stringResource(R.string.help_exam_export_desc)
                    )
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_training_title))
                Text(
                    text = stringResource(R.string.help_training_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow(
                        stringResource(R.string.help_training_theme_title),
                        stringResource(R.string.help_training_theme_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_training_confirm_title),
                        stringResource(R.string.help_training_confirm_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_training_source_title),
                        stringResource(R.string.help_training_source_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_training_progress_title),
                        stringResource(R.string.help_training_progress_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_training_reset_title),
                        stringResource(R.string.help_training_reset_desc)
                    )
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_achievements_title))
                Text(
                    text = stringResource(R.string.help_achievements_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow(
                        stringResource(R.string.help_achievements_exam_title),
                        stringResource(R.string.help_achievements_exam_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_achievements_training_title),
                        stringResource(R.string.help_achievements_training_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_achievements_page_title),
                        stringResource(R.string.help_achievements_page_desc)
                    )
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_settings_title))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow(
                        stringResource(R.string.help_settings_theme_title),
                        stringResource(R.string.help_settings_theme_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_settings_sound_title),
                        stringResource(R.string.help_settings_sound_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_settings_reset_training_title),
                        stringResource(R.string.help_settings_reset_training_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_settings_reset_cycle_title),
                        stringResource(R.string.help_settings_reset_cycle_desc)
                    )
                    HelpFeatureRow(
                        stringResource(R.string.help_settings_reset_achievements_title),
                        stringResource(R.string.help_settings_reset_achievements_desc)
                    )
                }
            }

            item {
                HelpSectionTitle(stringResource(R.string.help_links_title))
                Text(
                    text = stringResource(R.string.help_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OFFICIAL_LINKS.forEach { link ->
                        LinkCard(
                            label = stringResource(link.labelRes),
                            onClick = { uriHandler.openUri(link.url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun HelpRuleRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HelpFeatureRow(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkCard(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
