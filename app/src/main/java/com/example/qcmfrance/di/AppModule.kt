package com.example.qcmfrance.di

import android.content.Context
import com.example.qcmfrance.data.db.AppDatabase
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.repository.QuestionRepository
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
    fun provideQuestionRepository(dao: QuestionDao): QuestionRepository =
        QuestionRepository(dao)
}
