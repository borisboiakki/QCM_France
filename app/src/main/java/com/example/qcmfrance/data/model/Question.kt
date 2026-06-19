package com.example.qcmfrance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: Int,
    val theme: String,
    val text: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String,            // "A", "B", "C" ou "D"
    val correctAnswers: List<String>,     // toutes les réponses acceptées
    val explanation: String = ""
)
