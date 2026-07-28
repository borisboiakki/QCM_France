package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_results")
data class QuizResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val score: Int,
    val passed: Boolean,
    val totalQuestions: Int = 40,
    val durationSeconds: Int,
    /** QCM passé : code d'[ExamMode] ("NAT" par défaut pour les résultats antérieurs au multi-QCM). */
    val examMode: String = ExamMode.NATURALISATION.code
)
