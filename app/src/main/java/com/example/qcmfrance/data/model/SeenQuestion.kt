package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Id d'une question déjà apparue dans un examen soumis. Sert au succès « Tour complet »
 * ([Achievements.EXAM_ALL_SEEN]) : le nombre de lignes distinctes = nombre de questions vues.
 */
@Entity(tableName = "seen_question")
data class SeenQuestion(
    @PrimaryKey val questionId: Int
)
