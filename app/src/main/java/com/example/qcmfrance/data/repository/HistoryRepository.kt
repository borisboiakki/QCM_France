package com.example.qcmfrance.data.repository

import com.example.qcmfrance.data.db.QuizResultDao
import com.example.qcmfrance.data.model.QuizResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(private val dao: QuizResultDao) {

    val results: Flow<List<QuizResult>> = dao.getAll()

    suspend fun save(result: QuizResult) = dao.insert(result)

    suspend fun clearAll() = dao.deleteAll()
}
