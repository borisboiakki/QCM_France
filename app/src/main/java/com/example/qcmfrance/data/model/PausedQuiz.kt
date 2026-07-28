package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paused_quiz")
data class PausedQuiz(
    @PrimaryKey val id: Int = 1,
    val questionsJson: String,
    val answersJson: String,
    val currentIndex: Int,
    val remainingSeconds: Int,
    val savedAt: Long = System.currentTimeMillis(),
    /** QCM de l'examen en pause : code d'[ExamMode]. Restauré tel quel à la reprise. */
    val examMode: String = ExamMode.NATURALISATION.code
)
