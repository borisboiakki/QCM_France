package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Id d'une fiche thématique déjà consultée (écran de détail ouvert au moins une fois).
 *
 * Sert à deux choses :
 * - la barre d'avancement de lecture par thème dans l'écran « S'entraîner » (nombre de fiches lues
 *   d'un thème / total) ;
 * - les succès « Fiches officielles » ([Achievements.FICHE_FIRST_READ], [Achievements.FICHE_30_READ],
 *   [Achievements.FICHE_ALL_READ]) : le nombre de lignes distinctes = nombre de fiches consultées.
 *
 * L'id correspond à [Fiche.id] (unique sur tout le dataset : `"<slug-thème>__<slug-fiche>"`).
 */
@Entity(tableName = "read_fiche")
data class ReadFiche(
    @PrimaryKey val ficheId: String
)
