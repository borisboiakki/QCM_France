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
    val explanation: String = "",
    val source: String = "",
    val isSituation: Boolean = false,     // true = question de mise en situation
    val variants: List<QuestionVariant> = emptyList(),  // jeux de réponses alternatifs (rotation)
    // QCM auquel appartient la question : code d'ExamMode ("NAT", "CR", "CSP") pour les questions
    // de connaissances, ExamMode.SHARED_CODE ("ALL") pour les mises en situation, communes aux
    // trois modes. Renseigné par QuestionRepository.seedIfNeeded() selon le fichier de seed, pas
    // par le JSON lui-même.
    val examMode: String = ExamMode.NATURALISATION.code
)
