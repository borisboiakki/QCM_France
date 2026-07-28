package com.example.qcmfrance.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R

private data class ExternalLink(@StringRes val labelRes: Int, val url: String)

private data class LinkSection(@StringRes val titleRes: Int, val links: List<ExternalLink>)

private val LINK_SECTIONS = listOf(
    LinkSection(
        titleRes = R.string.resources_section_texts,
        links = listOf(
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
    ),
    LinkSection(
        titleRes = R.string.resources_section_exam,
        links = listOf(
            ExternalLink(
                labelRes = R.string.help_link_general,
                url = "https://formation-civique.interieur.gouv.fr/examen-civique/informations-g%C3%A9n%C3%A9rales-sur-lexamen-civique/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_factsheet,
                url = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/lexamen-civique-pour-demande-de-naturalisation-ou-de-reintegration-dans-nationalite-francaise.html"
            ),
            ExternalLink(
                labelRes = R.string.help_link_questions_cr,
                url = "https://formation-civique.interieur.gouv.fr/examen-civique/liste-officielle-des-questions-de-connaissance-cr/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_questions_csp,
                url = "https://formation-civique.interieur.gouv.fr/examen-civique/liste-officielle-des-questions-de-connaissance-csp/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_quiz,
                url = "https://www.ensemble-en-france.org/quiz-examen-civique-gratuit-debutant/"
            ),
        )
    ),
    LinkSection(
        titleRes = R.string.resources_section_sheets,
        links = listOf(
            ExternalLink(
                labelRes = R.string.help_link_sheet_droits,
                url = "https://formation-civique.interieur.gouv.fr/fiches-par-thematiques/droits-et-devoirs/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_sheet_histoire,
                url = "https://formation-civique.interieur.gouv.fr/fiches-par-thematiques/histoire-geographie-et-culture/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_sheet_principes,
                url = "https://formation-civique.interieur.gouv.fr/fiches-par-thematiques/principes-et-valeurs-de-la-republique/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_sheet_systeme,
                url = "https://formation-civique.interieur.gouv.fr/fiches-par-thematiques/systeme-institutionnel-et-politique/"
            ),
            ExternalLink(
                labelRes = R.string.help_link_sheet_societe,
                url = "https://formation-civique.interieur.gouv.fr/fiches-par-thematiques/vivre-dans-la-societe-fran%C3%A7aise/"
            ),
        )
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resources_title)) },
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.resources_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.resources_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LINK_SECTIONS.forEach { section ->
                item {
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(section.links) { link ->
                    LinkCard(
                        label = stringResource(link.labelRes),
                        onClick = { uriHandler.openUri(link.url) }
                    )
                }
            }
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
