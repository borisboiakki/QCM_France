package com.example.qcmfrance.data.repository

import android.content.Context
import com.example.qcmfrance.R
import com.example.qcmfrance.data.db.ExamCycleDao
import com.example.qcmfrance.data.db.QuestionDao
import com.example.qcmfrance.data.model.ExamCycle
import com.example.qcmfrance.data.model.ExamMode
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
    /**
     * Questions de connaissances par thème : 28 au total. Identique pour les trois QCM — chaque
     * liste officielle (naturalisation, CR, CSP) compte au moins 13 questions dans chaque thème,
     * le tirage est donc toujours satisfiable.
     */
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
         * v4 = multi-QCM : ajout des listes officielles carte de résident (`questions_cr.json`,
         * ids 2001+) et carte de séjour pluriannuelle (`questions_csp.json`, ids 3001+), et
         * renseignement de la colonne `examMode` sur toutes les questions.
         * v5 = lot 1 : rédaction des 31 questions manquantes du thème « Histoire, géographie et
         * culture » de la carte de résident (thème complet : 49/49).
         * v6 = lot 2 : rédaction des 35 questions manquantes du thème « Système institutionnel et
         * politique » de la carte de résident (thème complet : 50/50).
         */
        const val CONTENT_VERSION = 6
        private const val CONTENT_PREFS = "question_content"
        private const val KEY_CONTENT_VERSION = "content_version"
    }

    /**
     * Amorce **ou synchronise** la base depuis les quatre fichiers de seed : les listes de
     * connaissances des trois QCM ([R.raw.questions], [R.raw.questions_cr], [R.raw.questions_csp])
     * et les mises en situation ([R.raw.situational_questions]), communes aux trois modes.
     * Le QCM d'appartenance ([Question.examMode]) est déduit du fichier chargé, il n'est pas répété
     * dans le JSON. Séquentiel dans la coroutine appelante — pas de race condition. Réutilisé par
     * l'examen et l'entraînement (un utilisateur peut ouvrir l'entraînement avant d'avoir lancé un
     * examen).
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
            listOf(
                R.raw.questions to ExamMode.NATURALISATION.code,
                R.raw.questions_cr to ExamMode.RESIDENT_CARD.code,
                R.raw.questions_csp to ExamMode.MULTI_YEAR_PERMIT.code,
                R.raw.situational_questions to ExamMode.SHARED_CODE
            ).flatMap { (resId, mode) -> readSeedFile(resId, mode) }
        }
        dao.insertAll(questions)   // REPLACE : upsert par id (seed initial comme resynchro)
        prefs.edit().putInt(KEY_CONTENT_VERSION, CONTENT_VERSION).apply()
    }

    /**
     * Lit un fichier de seed et étiquette chaque question avec le QCM auquel le fichier correspond.
     *
     * Les instances sont **reconstruites** par le constructeur plutôt que copiées : Gson alloue les
     * [Question] sans passer par le constructeur Kotlin (allocation Unsafe), donc tout champ absent
     * du JSON est `null` en mémoire malgré son type non-null (cf. le même écueil documenté dans
     * `Converters`). Un `copy()` déclencherait alors une NullPointerException sur les 311 questions
     * dépourvues de clé `variants`. On en profite pour normaliser les champs optionnels.
     */
    @Suppress("USELESS_ELVIS")
    private fun readSeedFile(rawResId: Int, examMode: String): List<Question> {
        val type = object : TypeToken<List<Question>>() {}.type
        val parsed: List<Question> = gson.fromJson(
            context.resources.openRawResource(rawResId).bufferedReader().readText(), type
        )
        return parsed.map { q ->
            Question(
                id = q.id,
                theme = q.theme,
                text = q.text,
                optionA = q.optionA,
                optionB = q.optionB,
                optionC = q.optionC,
                optionD = q.optionD,
                correctAnswer = q.correctAnswer,
                explanation = q.explanation ?: "",
                source = q.source ?: "",
                isSituation = q.isSituation,
                variants = q.variants ?: emptyList(),
                examMode = examMode
            )
        }
    }

    /**
     * Tirage stratifié 28 connaissances + 12 mises en situation (6-9-6-13-6 par thème au
     * total, cf. [connaissanceCounts] / [situationCounts]) **dans le QCM demandé**, en cyclant par
     * thème **et par type** sur une permutation persistée (table `exam_cycle`) plutôt qu'un tirage
     * aléatoire indépendant à chaque appel : chaque question d'un thème/type est utilisée une fois
     * avant qu'une répétition ne survienne d'un examen à l'autre. Quand un cycle boucle (toutes ses
     * questions utilisées), une nouvelle permutation est générée pour le tour suivant.
     *
     * Chaque QCM a ses propres cycles (cf. [ExamMode.cycleKey]) : passer un examen carte de
     * résident ne consomme pas la rotation du mode naturalisation.
     */
    suspend fun drawStratifiedQuestions(mode: ExamMode): List<Question> {
        seedIfNeeded()

        val ids = mutableListOf<Int>()
        for ((theme, count) in connaissanceCounts) {
            ids += drawIdsFromCycle(theme, count, isSituation = false, mode = mode)
        }
        for ((theme, count) in situationCounts) {
            ids += drawIdsFromCycle(theme, count, isSituation = true, mode = mode)
        }
        return dao.getByIds(ids).shuffled()
    }

    /** Réinitialise le cycle de tirage de l'examen (tous QCM) : chaque clé repart d'une permutation neuve. */
    suspend fun resetExamCycle() = examCycleDao.clear()

    private suspend fun drawIdsFromCycle(
        theme: String,
        count: Int,
        isSituation: Boolean,
        mode: ExamMode
    ): List<Int> {
        if (count == 0) return emptyList()
        val allIds = dao.getIdsByTheme(theme, isSituation, mode.code)
        if (allIds.isEmpty()) return emptyList()

        // Clé de cycle distincte par QCM et par type de question : la table exam_cycle est indexée
        // par une chaîne libre, pas nécessairement le nom exact du thème, ce qui évite toute
        // migration de son schéma pour séparer ces cycles.
        // Un thème ne comptant pas encore assez de questions (QCM dont la liste est en cours de
        // saisie) en fournit autant qu'il peut, sans jamais tirer deux fois la même : `getByIds`
        // dédoublonne, un doublon se traduirait donc par un examen silencieusement plus court.
        val target = count.coerceAtMost(allIds.size)

        val cycleKey = mode.cycleKey(theme, isSituation)
        val saved = examCycleDao.get(cycleKey)
        val savedOrder = saved?.let { runCatching { gson.fromJson<List<Int>>(it.orderJson, idListType) }.getOrNull() }
        val reusable = savedOrder != null && savedOrder.toSet() == allIds.toSet()
        var order = if (reusable) savedOrder!! else allIds.shuffled()
        var cursor = if (reusable) saved!!.cursor.coerceIn(0, order.size) else 0

        val picked = mutableListOf<Int>()
        while (picked.size < target) {
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
