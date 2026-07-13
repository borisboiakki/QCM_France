package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.SeenQuestion

@Dao
interface SeenQuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SeenQuestion>)

    @Query("SELECT COUNT(*) FROM seen_question")
    suspend fun count(): Int

    @Query("DELETE FROM seen_question")
    suspend fun clear()
}
