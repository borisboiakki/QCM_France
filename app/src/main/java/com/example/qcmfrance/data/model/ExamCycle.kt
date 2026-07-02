package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cycle de tirage de l'examen blanc, une ligne par thème.
 *
 * [orderJson] est une permutation (JSON, Gson) des ids de toutes les questions du thème.
 * [cursor] est l'index (0-based) de la prochaine question à tirer dans cette permutation.
 * Tirer les questions dans cet ordre, en avançant le curseur à chaque examen, garantit que
 * toutes les questions d'un thème sont utilisées une fois avant qu'une répétition survienne.
 */
@Entity(tableName = "exam_cycle")
data class ExamCycle(
    @PrimaryKey val theme: String,
    val orderJson: String,
    val cursor: Int
)
