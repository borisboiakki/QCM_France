package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.ReadFiche
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadFicheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ReadFiche)

    /** Ids des fiches déjà consultées, observés pour la progression par thème (X/total). */
    @Query("SELECT ficheId FROM read_fiche")
    fun observeReadIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM read_fiche")
    suspend fun count(): Int

    @Query("DELETE FROM read_fiche")
    suspend fun clear()
}
