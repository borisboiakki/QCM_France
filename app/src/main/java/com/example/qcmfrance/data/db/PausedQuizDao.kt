package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.PausedQuiz
import kotlinx.coroutines.flow.Flow

@Dao
interface PausedQuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(pausedQuiz: PausedQuiz)

    @Query("SELECT * FROM paused_quiz WHERE id = 1")
    suspend fun get(): PausedQuiz?

    @Query("SELECT * FROM paused_quiz WHERE id = 1")
    fun observe(): Flow<PausedQuiz?>

    @Query("DELETE FROM paused_quiz WHERE id = 1")
    suspend fun delete()
}
