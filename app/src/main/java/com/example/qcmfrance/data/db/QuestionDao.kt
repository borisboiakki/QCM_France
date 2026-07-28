package com.example.qcmfrance.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.qcmfrance.data.model.Question

/**
 * Accès aux questions. Toutes les requêtes filtrées par QCM retiennent les questions de
 * connaissances du mode demandé **et** les mises en situation (`isSituation = 1`), qui sont
 * communes aux trois modes.
 */
@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE theme = :theme AND (examMode = :mode OR isSituation = 1) ORDER BY id")
    suspend fun getAllByTheme(theme: String, mode: String): List<Question>

    @Query("SELECT id FROM questions WHERE theme = :theme AND isSituation = :isSituation AND (examMode = :mode OR isSituation = 1) ORDER BY id")
    suspend fun getIdsByTheme(theme: String, isSituation: Boolean, mode: String): List<Int>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    /** Nombre de questions tirables dans un QCM : cible du succès « Tour complet » du mode. */
    @Query("SELECT COUNT(*) FROM questions WHERE examMode = :mode OR isSituation = 1")
    suspend fun countForMode(mode: String): Int

    /** Nombre de questions de ce QCM déjà vues en examen : progression du succès « Tour complet ». */
    @Query(
        """
        SELECT COUNT(*) FROM seen_question s
        INNER JOIN questions q ON q.id = s.questionId
        WHERE q.examMode = :mode OR q.isSituation = 1
        """
    )
    suspend fun countSeenForMode(mode: String): Int
}
