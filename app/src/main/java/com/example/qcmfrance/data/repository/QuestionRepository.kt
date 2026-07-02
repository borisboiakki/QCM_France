package com.example.qcmfrance.data.repository

import android.content.Context
import com.example.qcmfrance.R
import com.example.qcmfrance.data.db.ExamCycleDao
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.model.ExamCycle
import com.example.qcmfrance.data.model.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val dao: QuestionDao,
    private val examCycleDao: ExamCycleDao,
    @ApplicationContext private val context: Context
) {
    private val themeCounts = mapOf(
        "Principes et valeurs de la République" to 6,
        "Système institutionnel et politique"   to 9,
        "Droits et devoirs"                     to 6,
        "Histoire, géographie et culture"       to 13,
        "Vivre dans la société française"       to 6
    )

    /** Les 5 thèmes officiels, dans l'ordre, pour l'écran de sélection de l'entraînement. */
    val themes: List<String> = themeCounts.keys.toList()

    private val gson = Gson()
    private val idListType = object : TypeToken<List<Int>>() {}.type

    /**
     * Amorce la base depuis [R.raw.questions] au tout premier accès. Séquentiel dans la
     * coroutine appelante — pas de race condition. Réutilisé par l'examen et l'entraînement
     * (un utilisateur peut ouvrir l'entraînement avant d'avoir lancé un examen).
     */
    suspend fun seedIfNeeded() {
        if (dao.count() == 0) {
            val questions: List<Question> = withContext(Dispatchers.IO) {
                val json = context.resources.openRawResource(R.raw.questions)
                    .bufferedReader().readText()
                val type = object : TypeToken<List<Question>>() {}.type
                Gson().fromJson(json, type)
            }
            dao.insertAll(questions)
        }
    }

    /**
     * Tirage stratifié 6-9-6-13-6, en cyclant par thème sur une permutation persistée
     * (table `exam_cycle`) plutôt qu'un tirage aléatoire indépendant à chaque appel : chaque
     * question d'un thème est utilisée une fois avant qu'une répétition ne survienne d'un
     * examen à l'autre. Quand un thème boucle (toutes ses questions utilisées), une nouvelle
     * permutation est générée pour le tour suivant.
     */
    suspend fun drawStratifiedQuestions(): List<Question> {
        seedIfNeeded()

        val ids = mutableListOf<Int>()
        for ((theme, count) in themeCounts) {
            ids += drawIdsFromCycle(theme, count)
        }
        return dao.getByIds(ids).shuffled()
    }

    /** Réinitialise le cycle de tirage de l'examen : chaque thème repart d'une permutation neuve. */
    suspend fun resetExamCycle() = examCycleDao.clear()

    private suspend fun drawIdsFromCycle(theme: String, count: Int): List<Int> {
        val allIds = dao.getIdsByTheme(theme)
        if (allIds.isEmpty()) return emptyList()

        val saved = examCycleDao.get(theme)
        val savedOrder = saved?.let { runCatching { gson.fromJson<List<Int>>(it.orderJson, idListType) }.getOrNull() }
        val reusable = savedOrder != null && savedOrder.toSet() == allIds.toSet()
        var order = if (reusable) savedOrder!! else allIds.shuffled()
        var cursor = if (reusable) saved!!.cursor.coerceIn(0, order.size) else 0

        val picked = mutableListOf<Int>()
        while (picked.size < count) {
            if (cursor >= order.size) {
                // Fin d'un tour : nouvelle permutation pour le suivant, sans dupliquer dans ce
                // tirage les ids déjà pris juste avant le bouclage.
                val excluded = picked.toSet()
                order = allIds.filterNot { it in excluded }.shuffled() + excluded.shuffled()
                cursor = 0
            }
            picked += order[cursor]
            cursor++
        }

        examCycleDao.save(ExamCycle(theme = theme, orderJson = gson.toJson(order), cursor = cursor))
        return picked
    }
}
