package com.example.qcmfrance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qcmfrance.data.model.PausedQuiz
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.QuizResult

@Database(entities = [Question::class, QuizResult::class, PausedQuiz::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun pausedQuizDao(): PausedQuizDao

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

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "qcm_france.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
