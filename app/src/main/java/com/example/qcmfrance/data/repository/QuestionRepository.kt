package com.example.qcmfrance.data.repository

import android.content.Context
import com.example.qcmfrance.R
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.model.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val dao: QuestionDao,
    @ApplicationContext private val context: Context
) {
    private val themeCounts = mapOf(
        "Principes et valeurs de la République" to 6,
        "Système institutionnel et politique"   to 9,
        "Droits et devoirs"                     to 6,
        "Histoire, géographie et culture"       to 13,
        "Vivre dans la société française"       to 6
    )

    suspend fun drawStratifiedQuestions(): List<Question> {
        // Seed au premier lancement — séquentiel, pas de race condition
        if (dao.count() == 0) {
            val json = context.resources.openRawResource(R.raw.questions)
                .bufferedReader().readText()
            val type = object : TypeToken<List<Question>>() {}.type
            val questions: List<Question> = Gson().fromJson(json, type)
            dao.insertAll(questions)
        }

        val questions = mutableListOf<Question>()
        for ((theme, count) in themeCounts) {
            questions += dao.getRandomByTheme(theme, count)
        }
        return questions.shuffled()
    }
}
