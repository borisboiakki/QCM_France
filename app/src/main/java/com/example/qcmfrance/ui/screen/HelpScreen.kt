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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ExternalLink(val label: String, val url: String)

private val OFFICIAL_LINKS = listOf(
    ExternalLink(
        label = "Informations générales sur l'examen civique",
        url   = "https://formation-civique.interieur.gouv.fr/examen-civique/informations-g%C3%A9n%C3%A9rales-sur-lexamen-civique/"
    ),
    ExternalLink(
        label = "Tests complémentaires en ligne (Ensemble en France)",
        url   = "https://www.ensemble-en-france.org/quiz-examen-civique-gratuit-debutant/"
    ),
    ExternalLink(
        label = "Fiche d'information sur l'examen",
        url   = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/lexamen-civique-pour-demande-de-naturalisation-ou-de-reintegration-dans-nationalite-francaise.html"
    ),
    ExternalLink(
        label = "Livret du citoyen",
        url   = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/livret-du-citoyen.html"
    ),
    ExternalLink(
        label = "Charte des droits et devoirs du citoyen français",
        url   = "https://www.immigration.interieur.gouv.fr/documentation/guides-textes-et-brochures/charte-des-droits-et-devoirs-du-citoyen-francais.html"
    ),
    ExternalLink(
        label = "Déclaration des droits de l'homme et du citoyen (1789)",
        url   = "https://www.conseil-constitutionnel.fr/le-bloc-de-constitutionnalite/declaration-des-droits-de-l-homme-et-du-citoyen-de-1789"
    ),
    ExternalLink(
        label = "Constitution française (1958)",
        url   = "https://www.legifrance.gouv.fr/loda/id/JORFTEXT000000571356/"
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aide") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HelpSectionTitle("À propos")
                Text(
                    text = "QCM France vous aide à préparer l'examen civique de naturalisation française. " +
                        "L'application propose deux modes complémentaires :\n\n" +
                        "• Mode examen : 40 questions tirées aléatoirement, 45 minutes au chronomètre, sans correction immédiate — comme dans les conditions officielles.\n\n" +
                        "• Mode S'entraîner : parcourez toutes les questions d'un thème, une par une, avec correction immédiate, explication et lien vers la source après chaque réponse confirmée.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                HelpSectionTitle("Règles de l'examen officiel")
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
                        HelpRuleRow("Nombre de questions", "40")
                        HelpRuleRow("Seuil de réussite",   "32 / 40 (80 %)")
                        HelpRuleRow("Durée maximale",       "45 minutes")
                        HelpRuleRow("Format",               "1 bonne réponse parmi 4")
                        HelpRuleRow("Résultats",            "Affichés à la fin uniquement")
                    }
                }
            }

            item {
                HelpSectionTitle("Thèmes couverts")
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
                        HelpRuleRow("Principes et valeurs de la République",   "6 questions")
                        HelpRuleRow("Système institutionnel et politique",      "9 questions")
                        HelpRuleRow("Droits et devoirs",                        "6 questions")
                        HelpRuleRow("Histoire, géographie et culture",          "13 questions")
                        HelpRuleRow("Vivre dans la société française",          "6 questions")
                    }
                }
            }

            item {
                HelpSectionTitle("Mode examen")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow("Chronomètre", "Décompte de 45 min, affiché en rouge dans les 5 dernières minutes. Soumission automatique à 00:00.")
                    HelpFeatureRow("Pause et reprise", "Mettez l'examen en pause via le bouton Pause ou la touche Retour. L'état est sauvegardé et vous pouvez reprendre plus tard, même après avoir fermé l'application.")
                    HelpFeatureRow("Historique", "Consultez tous vos examens passés avec le score, la durée et la mention obtenue.")
                    HelpFeatureRow("Export des résultats", "Partagez le détail de votre examen (question par question) via les applications installées sur votre téléphone.")
                }
            }

            item {
                HelpSectionTitle("Mode S'entraîner")
                Text(
                    text = "Un mode d'apprentissage complémentaire à l'examen, sans chronomètre et avec correction immédiate.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow("Choix du thème", "Sélectionnez l'un des 5 thèmes officiels. Une barre de progression indique combien de questions vous avez déjà vues dans ce thème.")
                    HelpFeatureRow("Confirmer pour révéler", "Choisissez une réponse — vous pouvez la modifier tant que vous n'avez pas appuyé sur « Confirmer ». Une fois confirmée, la bonne réponse apparaît en vert et une mauvaise sélection en rouge.")
                    HelpFeatureRow("Explication et source", "Après confirmation, une explication est affichée (si disponible) ainsi qu'un lien vers la source officielle, ouvrant votre navigateur.")
                    HelpFeatureRow("Avancement persisté", "Votre progression est sauvegardée question par question. Revenir en arrière ou fermer l'application ne fait pas perdre votre avancement — le thème reprend exactement là où vous vous étiez arrêté.")
                    HelpFeatureRow("Réinitialisation", "Depuis les Paramètres, réinitialisez la progression de tous les thèmes en une seule action.")
                }
            }

            item {
                HelpSectionTitle("Paramètres")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HelpFeatureRow("Thème visuel", "Choisissez entre le thème du système, clair ou sombre — persisté entre les sessions.")
                    HelpFeatureRow("Son de sélection", "Activez ou désactivez le bip joué lors de la sélection d'une réponse en examen.")
                    HelpFeatureRow("Réinitialiser l'entraînement", "Efface la progression de tous les thèmes du mode S'entraîner (action irréversible, avec confirmation).")
                }
            }

            item {
                HelpSectionTitle("Ressources officielles")
                Text(
                    text = "Appuyez sur un lien pour l'ouvrir dans votre navigateur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OFFICIAL_LINKS.forEach { link ->
                        LinkCard(label = link.label, onClick = { uriHandler.openUri(link.url) })
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
