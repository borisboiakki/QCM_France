package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.AchievementRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementRecord>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun get(id: String): AchievementRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AchievementRecord)

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
