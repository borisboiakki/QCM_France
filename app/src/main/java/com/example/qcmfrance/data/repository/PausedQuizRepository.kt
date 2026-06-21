package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.PausedQuizDao
import com.example.qcmfrance.data.model.PausedQuiz
import com.example.qcmfrance.data.model.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PausedQuizState(
    val questions: List<Question>,
    val answers: Map<Int, String>,
    val currentIndex: Int,
    val remainingSeconds: Int
)

@Singleton
class PausedQuizRepository @Inject constructor(private val dao: PausedQuizDao) {

    private val gson = Gson()
    private val questionListType = object : TypeToken<List<Question>>() {}.type
    private val answersMapType = object : TypeToken<Map<Int, String>>() {}.type

    suspend fun save(
        questions: List<Question>,
        answers: Map<Int, String>,
        currentIndex: Int,
        remainingSeconds: Int
    ) {
        dao.save(
            PausedQuiz(
                questionsJson    = gson.toJson(questions),
                answersJson      = gson.toJson(answers),
                currentIndex     = currentIndex,
                remainingSeconds = remainingSeconds
            )
        )
    }

    suspend fun load(): PausedQuizState? {
        val row = dao.get() ?: return null
        return PausedQuizState(
            questions        = gson.fromJson(row.questionsJson, questionListType),
            answers          = gson.fromJson(row.answersJson, answersMapType),
            currentIndex     = row.currentIndex,
            remainingSeconds = row.remainingSeconds
        )
    }

    fun observeHasPaused(): Flow<Boolean> = dao.observe().map { it != null }

    suspend fun hasPaused(): Boolean = dao.get() != null

    suspend fun clear() = dao.delete()
}
