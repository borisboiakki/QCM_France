package com.example.qcmfrance.di

import android.content.Context
import com.example.qcmfrance.data.db.AppDatabase
import com.example.qcmfrance.data.db.ExamCycleDao
import com.example.qcmfrance.data.db.PausedQuizDao
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.db.QuizResultDao
import com.example.qcmfrance.data.db.TrainingProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    @Singleton
    fun provideQuizResultDao(db: AppDatabase): QuizResultDao = db.quizResultDao()

    @Provides
    @Singleton
    fun providePausedQuizDao(db: AppDatabase): PausedQuizDao = db.pausedQuizDao()

    @Provides
    @Singleton
    fun provideTrainingProgressDao(db: AppDatabase): TrainingProgressDao = db.trainingProgressDao()

    @Provides
    @Singleton
    fun provideExamCycleDao(db: AppDatabase): ExamCycleDao = db.examCycleDao()
}
