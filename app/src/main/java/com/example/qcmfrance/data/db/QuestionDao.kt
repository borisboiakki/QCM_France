package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.Question

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomByTheme(theme: String, count: Int): List<Question>

    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY id")
    suspend fun getAllByTheme(theme: String): List<Question>

    @Query("SELECT COUNT(*) FROM questions WHERE theme = :theme")
    suspend fun countByTheme(theme: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int
}
