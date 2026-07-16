package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.Question

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE theme = :theme ORDER BY id")
    suspend fun getAllByTheme(theme: String): List<Question>

    @Query("SELECT id FROM questions WHERE theme = :theme AND isSituation = :isSituation ORDER BY id")
    suspend fun getIdsByTheme(theme: String, isSituation: Boolean): List<Int>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int
}
