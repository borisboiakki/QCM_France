package com.example.qcmfrance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qcmfrance.data.model.ExamCycle
import com.example.qcmfrance.data.model.PausedQuiz
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.QuizResult
import com.example.qcmfrance.data.model.TrainingProgress

@Database(
    entities = [Question::class, QuizResult::class, PausedQuiz::class, TrainingProgress::class, ExamCycle::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun pausedQuizDao(): PausedQuizDao
    abstract fun trainingProgressDao(): TrainingProgressDao
    abstract fun examCycleDao(): ExamCycleDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        score INTEGER NOT NULL,
                        passed INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL DEFAULT 40,
                        durationSeconds INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                // Wipe rows so the repository re-seeds from JSON (which carries the source URLs)
                database.execSQL("DELETE FROM questions")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS paused_quiz (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        questionsJson TEXT NOT NULL,
                        answersJson TEXT NOT NULL,
                        currentIndex INTEGER NOT NULL,
                        remainingSeconds INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_progress (
                        theme TEXT PRIMARY KEY NOT NULL,
                        currentIndex INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exam_cycle (
                        theme TEXT PRIMARY KEY NOT NULL,
                        orderJson TEXT NOT NULL,
                        cursor INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Suppression de la colonne correctAnswers (jamais lue par l'app). SQLite ne supporte
        // pas DROP COLUMN sur toutes les versions d'Android : recréation de la table.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS questions_new (
                        id INTEGER NOT NULL,
                        theme TEXT NOT NULL,
                        text TEXT NOT NULL,
                        optionA TEXT NOT NULL,
                        optionB TEXT NOT NULL,
                        optionC TEXT NOT NULL,
                        optionD TEXT NOT NULL,
                        correctAnswer TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        source TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO questions_new (id, theme, text, optionA, optionB, optionC, optionD, correctAnswer, explanation, source)
                    SELECT id, theme, text, optionA, optionB, optionC, optionD, correctAnswer, explanation, source FROM questions
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE questions")
                database.execSQL("ALTER TABLE questions_new RENAME TO questions")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "qcm_france.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
    }
}
