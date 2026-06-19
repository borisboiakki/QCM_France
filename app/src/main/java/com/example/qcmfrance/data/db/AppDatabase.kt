package com.example.qcmfrance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qcmfrance.R
import com.example.qcmfrance.data.model.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Question::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    companion object {
        fun build(context: Context): AppDatabase {
            var instance: AppDatabase? = null
            instance = Room.databaseBuilder(context, AppDatabase::class.java, "qcm_france.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val json = context.resources.openRawResource(R.raw.questions)
                                .bufferedReader().readText()
                            val type = object : TypeToken<List<Question>>() {}.type
                            val questions: List<Question> = Gson().fromJson(json, type)
                            instance!!.questionDao().insertAll(questions)
                        }
                    }
                })
                .build()
            return instance
        }
    }
}
