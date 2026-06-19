package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.model.Question
import javax.inject.Inject

class QuestionRepository @Inject constructor(
    private val dao: QuestionDao
) {
    private val themeCounts = mapOf(
        "Principes et valeurs de la République" to 6,
        "Système institutionnel et politique"   to 9,
        "Droits et devoirs"                     to 6,
        "Histoire, géographie et culture"       to 13,
        "Vivre dans la société française"       to 6
    )

    suspend fun drawStratifiedQuestions(): List<Question> {
        val questions = mutableListOf<Question>()
        for ((theme, count) in themeCounts) {
            questions += dao.getRandomByTheme(theme, count)
        }
        return questions.shuffled()
    }
}
