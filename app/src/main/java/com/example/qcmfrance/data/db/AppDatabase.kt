package com.example.qcmfrance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.QuizResult

@Database(entities = [Question::class, QuizResult::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun quizResultDao(): QuizResultDao

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

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "qcm_france.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
