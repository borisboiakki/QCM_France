package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Avancement de l'utilisateur dans le mode « S'entraîner », une ligne par thème.
 *
 * [currentIndex] est l'index 0-based de la prochaine question à présenter dans le thème
 * (= nombre de questions déjà parcourues). Il sert à la fois de point de reprise et de
 * valeur « X » dans la barre de progression X/total. Le thème est considéré terminé
 * lorsque [currentIndex] atteint le nombre total de questions du thème.
 */
@Entity(tableName = "training_progress")
data class TrainingProgress(
    @PrimaryKey val theme: String,
    val currentIndex: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
