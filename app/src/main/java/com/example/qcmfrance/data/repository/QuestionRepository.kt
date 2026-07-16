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
    /** Questions de connaissances par thème : 28 au total. */
    private val connaissanceCounts = mapOf(
        "Principes et valeurs de la République" to 3,
        "Système institutionnel et politique"   to 6,
        "Droits et devoirs"                     to 3,
        "Histoire, géographie et culture"       to 13,
        "Vivre dans la société française"       to 3
    )

    /**
     * Questions de mise en situation par thème : 12 au total. Le thème « Histoire, géographie
     * et culture » n'en a pas (aucune question de mise en situation adaptée à ce thème).
     */
    private val situationCounts = mapOf(
        "Principes et valeurs de la République" to 3,
        "Système institutionnel et politique"   to 3,
        "Droits et devoirs"                     to 3,
        "Histoire, géographie et culture"       to 0,
        "Vivre dans la société française"       to 3
    )

    /** Les 5 thèmes officiels, dans l'ordre, pour l'écran de sélection de l'entraînement. */
    val themes: List<String> = connaissanceCounts.keys.toList()

    private val gson = Gson()
    private val idListType = object : TypeToken<List<Int>>() {}.type

    companion object {
        /**
         * Version du contenu des questions (fichiers JSON de seed). À **incrémenter** dès qu'une
         * question est corrigée/ajoutée pour que [seedIfNeeded] resynchronise la base des apps
         * déjà installées. Historique : v1 = corrections libellés (guillemets q. 97, « 17 » q. 222…).
         * v2 = q. 89 (traité UE) : bonne réponse simplifiée en « Traité de Maastricht » et
         * distracteurs Rome/Paris (eux-mêmes fondateurs de la construction européenne) remplacés
         * par Versailles/Vienne/Westphalie pour lever l'ambiguïté.
         * v3 = ajout de variantes de réponses (champ `variants`) sur les questions à plusieurs
         * bonnes réponses valides (musées de Paris, peintres français, colonies…) : le jeu de
         * réponses affiché tourne aléatoirement à chaque examen/entraînement. Inclut aussi la
         * correction de 3 distracteurs eux-mêmes défendables comme bonnes réponses : q. 139
         * (Provence → Aquitaine, débarquement de Provence d'août 1944), q. 162 (UNESCO → Conseil
         * de l'Europe, l'UNESCO datant aussi de 1945), q. 166 (Rousseau → Colbert, Rousseau ayant
         * aussi dénoncé l'esclavage). Inclut enfin 20 mises en situation supplémentaires
         * (ids 1061-1080, +5 par thème concerné : 80 au total).
         */
        const val CONTENT_VERSION = 3
        private const val CONTENT_PREFS = "question_content"
        private const val KEY_CONTENT_VERSION = "content_version"
    }

    /**
     * Amorce **ou synchronise** la base depuis [R.raw.questions] (connaissances) et
     * [R.raw.situational_questions] (mises en situation). Séquentiel dans la coroutine appelante —
     * pas de race condition. Réutilisé par l'examen et l'entraînement (un utilisateur peut ouvrir
     * l'entraînement avant d'avoir lancé un examen).
     *
     * Deux cas déclenchent une (ré)écriture :
     *  - **Premier lancement** (`count() == 0`) : insertion complète.
     *  - **Contenu obsolète** : la version de contenu appliquée (stockée en [SharedPreferences],
     *    donc sans migration Room) est inférieure à [CONTENT_VERSION]. Le JSON est alors ré-appliqué
     *    en `INSERT OR REPLACE` (upsert par id, cf. [QuestionDao.insertAll]), ce qui met à jour les
     *    libellés/corrections des questions existantes et ajoute les nouvelles, **sans toucher** aux
     *    autres tables (historique, succès, progression d'entraînement, cycle d'examen). Les installs
     *    antérieures à cette fonctionnalité n'ont pas de version stockée (défaut 0 < [CONTENT_VERSION])
     *    et bénéficient donc d'une resynchronisation unique.
     *
     * Bump [CONTENT_VERSION] à **chaque** modification de `questions.json` /
     * `situational_questions.json` pour propager le changement aux apps déjà installées.
     */
    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
        val appliedVersion = prefs.getInt(KEY_CONTENT_VERSION, 0)
        val isEmpty = dao.count() == 0
        if (!isEmpty && appliedVersion >= CONTENT_VERSION) return

        val questions: List<Question> = withContext(Dispatchers.IO) {
            val type = object : TypeToken<List<Question>>() {}.type
            val connaissance: List<Question> = gson.fromJson(
                context.resources.openRawResource(R.raw.questions).bufferedReader().readText(), type
            )
            val situation: List<Question> = gson.fromJson(
                context.resources.openRawResource(R.raw.situational_questions).bufferedReader().readText(), type
            )
            connaissance + situation
        }
        dao.insertAll(questions)   // REPLACE : upsert par id (seed initial comme resynchro)
        prefs.edit().putInt(KEY_CONTENT_VERSION, CONTENT_VERSION).apply()
    }

    /**
     * Tirage stratifié 28 connaissances + 12 mises en situation (6-9-6-13-6 par thème au
     * total, cf. [connaissanceCounts] / [situationCounts]), en cyclant par thème **et par
     * type** sur une permutation persistée (table `exam_cycle`) plutôt qu'un tirage aléatoire
     * indépendant à chaque appel : chaque question d'un thème/type est utilisée une fois avant
     * qu'une répétition ne survienne d'un examen à l'autre. Quand un cycle boucle (toutes ses
     * questions utilisées), une nouvelle permutation est générée pour le tour suivant.
     */
    suspend fun drawStratifiedQuestions(): List<Question> {
        seedIfNeeded()

        val ids = mutableListOf<Int>()
        for ((theme, count) in connaissanceCounts) {
            ids += drawIdsFromCycle(theme, count, isSituation = false)
        }
        for ((theme, count) in situationCounts) {
            ids += drawIdsFromCycle(theme, count, isSituation = true)
        }
        return dao.getByIds(ids).shuffled()
    }

    /** Réinitialise le cycle de tirage de l'examen : chaque thème repart d'une permutation neuve. */
    suspend fun resetExamCycle() = examCycleDao.clear()

    private suspend fun drawIdsFromCycle(theme: String, count: Int, isSituation: Boolean): List<Int> {
        if (count == 0) return emptyList()
        val allIds = dao.getIdsByTheme(theme, isSituation)
        if (allIds.isEmpty()) return emptyList()

        // Clé de cycle distincte pour les mises en situation : la table exam_cycle est indexée
        // par une chaîne libre, pas nécessairement le nom exact du thème, ce qui évite toute
        // migration de son schéma pour séparer les deux cycles d'un même thème.
        val cycleKey = if (isSituation) "$theme::situation" else theme
        val saved = examCycleDao.get(cycleKey)
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

        examCycleDao.save(ExamCycle(theme = cycleKey, orderJson = gson.toJson(order), cursor = cursor))
        return picked
    }
}
