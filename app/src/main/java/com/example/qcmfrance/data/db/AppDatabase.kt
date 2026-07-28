package com.example.qcmfrance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qcmfrance.data.model.AchievementRecord
import com.example.qcmfrance.data.model.ExamCycle
import com.example.qcmfrance.data.model.PausedQuiz
import com.example.qcmfrance.data.model.Question
import com.example.qcmfrance.data.model.QuizResult
import com.example.qcmfrance.data.model.ReadFiche
import com.example.qcmfrance.data.model.SeenQuestion
import com.example.qcmfrance.data.model.TrainingProgress

@Database(
    entities = [Question::class, QuizResult::class, PausedQuiz::class, TrainingProgress::class, ExamCycle::class, AchievementRecord::class, SeenQuestion::class, ReadFiche::class],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun pausedQuizDao(): PausedQuizDao
    abstract fun trainingProgressDao(): TrainingProgressDao
    abstract fun examCycleDao(): ExamCycleDao
    abstract fun achievementDao(): AchievementDao
    abstract fun seenQuestionDao(): SeenQuestionDao
    abstract fun readFicheDao(): ReadFicheDao

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

        // Ajout des questions de mise en situation (colonne isSituation) : wipe pour reseed
        // complet depuis questions.json + situational_questions.json (même pattern que 2->3).
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN isSituation INTEGER NOT NULL DEFAULT 0")
                database.execSQL("DELETE FROM questions")
            }
        }

        // Ajout du système de succès : tables achievements + seen_question (questions déjà vues
        // en examen, pour le succès « Tour complet »).
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS achievements (
                        id TEXT PRIMARY KEY NOT NULL,
                        unlockedAt INTEGER,
                        progress INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS seen_question (
                        questionId INTEGER PRIMARY KEY NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Ajout des variantes de réponses : nouvelle colonne `variants` (JSON) sur `questions`.
        // Défaut '[]' ; le seed la peuple ensuite via CONTENT_VERSION (INSERT OR REPLACE).
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN variants TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Suivi de lecture des fiches thématiques hors-ligne : table read_fiche (id de fiche déjà
        // consultée) — barre d'avancement par thème + succès « Fiches officielles ».
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS read_fiche (
                        ficheId TEXT PRIMARY KEY NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Multi-QCM (naturalisation / carte de résident / carte de séjour pluriannuelle) : chaque
        // question de connaissances appartient à un QCM, et chaque examen (en cours comme terminé)
        // mémorise celui qu'il porte. Le défaut 'NAT' laisse les données existantes cohérentes ;
        // la colonne des questions est ensuite peuplée par le seed (CONTENT_VERSION).
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE questions ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT'")
                database.execSQL("ALTER TABLE paused_quiz ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT'")
                database.execSQL("ALTER TABLE quiz_results ADD COLUMN examMode TEXT NOT NULL DEFAULT 'NAT'")
                // Les mises en situation sont communes aux trois QCM.
                database.execSQL("UPDATE questions SET examMode = 'ALL' WHERE isSituation = 1")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "qcm_france.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .build()
    }
}
