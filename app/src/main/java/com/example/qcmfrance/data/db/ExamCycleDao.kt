package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.ExamCycle

@Dao
interface ExamCycleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cycle: ExamCycle)

    @Query("SELECT * FROM exam_cycle WHERE theme = :theme")
    suspend fun get(theme: String): ExamCycle?

    @Query("DELETE FROM exam_cycle")
    suspend fun clear()
}
