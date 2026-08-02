package com.example.qcmfrance.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.qcmfrance.R

/**
 * Badge « Mise en situation ».
 *
 * Les 12 mises en situation sont posées à la fin de l'examen (cf.
 * `QuestionRepository.drawStratifiedQuestions`) ; ce badge les distingue des questions de
 * connaissances, sur l'écran d'examen comme dans le détail de l'écran résultat.
 *
 * Couleurs issues du thème Material 3 — lisible en clair comme en sombre.
 */
@Composable
fun SituationBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = stringResource(R.string.quiz_situation_badge),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
