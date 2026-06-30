package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.TrainingProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: TrainingProgress)

    @Query("SELECT * FROM training_progress WHERE theme = :theme")
    suspend fun get(theme: String): TrainingProgress?

    @Query("SELECT * FROM training_progress")
    fun observeAll(): Flow<List<TrainingProgress>>

    @Query("DELETE FROM training_progress")
    suspend fun clear()
}
