package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.qcmfrance.data.model.QuizResult
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizResultDao {

    @Query("SELECT * FROM quiz_results ORDER BY date DESC")
    fun getAll(): Flow<List<QuizResult>>

    @Insert
    suspend fun insert(result: QuizResult)

    @Query("DELETE FROM quiz_results")
    suspend fun deleteAll()
}
