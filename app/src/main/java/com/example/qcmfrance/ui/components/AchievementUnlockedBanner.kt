package com.example.qcmfrance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R
import com.example.qcmfrance.data.model.Achievement
import com.example.qcmfrance.data.model.AchievementRarity
import com.example.qcmfrance.ui.theme.AchievementGold
import kotlinx.coroutines.delay

private const val BANNER_DURATION_MS = 4_000L

/**
 * Bandeau « Succès débloqué ! » façon jeu vidéo : une carte qui glisse depuis le haut de l'écran,
 * liseré doré pour les succès rares, auto-dismiss après [BANNER_DURATION_MS] (ou au clic).
 *
 * Rendu au-dessus de la navigation (overlay global) : [achievement] pointe la tête de file des
 * succès à afficher, [onDismiss] retire cette tête pour laisser place au suivant.
 */
@Composable
fun AchievementUnlockedBanner(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Conserve le dernier succès affiché pour continuer à le rendre pendant l'animation de sortie.
    var shown by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(achievement) {
        if (achievement != null) {
            shown = achievement
            delay(BANNER_DURATION_MS)
            onDismiss()
        }
    }

    val isRare = shown?.rarity == AchievementRarity.RARE
    val border = if (isRare) BorderStroke(2.dp, AchievementGold) else null

    AnimatedVisibility(
        visible = achievement != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val current = shown ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = border
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = current.emoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.achievements_banner_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isRare) AchievementGold else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(current.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
