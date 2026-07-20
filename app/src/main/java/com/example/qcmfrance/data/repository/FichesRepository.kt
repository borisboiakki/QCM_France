package com.example.qcmfrance.data.repository

import android.content.Context
import com.example.qcmfrance.R
import com.example.qcmfrance.data.db.ReadFicheDao
import com.example.qcmfrance.data.model.Fiche
import com.example.qcmfrance.data.model.FicheTheme
import com.example.qcmfrance.data.model.FichesData
import com.example.qcmfrance.data.model.ReadFiche
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fournit les fiches thématiques officielles pour la consultation **hors-ligne**.
 *
 * Lit `res/raw/fiches.json` (généré par la pipeline `update-fiches`) via Gson, sur [Dispatchers.IO],
 * et met le résultat en cache mémoire. Aucune persistance Room : le contenu bundlé est la source de
 * vérité (cf. [com.example.qcmfrance.data.model.FichesData]).
 */
@Singleton
class FichesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readFicheDao: ReadFicheDao
) {
    private val gson = Gson()
    private val mutex = Mutex()
    @Volatile private var cache: FichesData? = null

    private suspend fun data(): FichesData {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) {
                // Défensif : toute erreur de lecture/parse (ressource absente si mal shrinkée en
                // release, JSON malformé…) renvoie un dataset vide plutôt que de crasher l'app.
                runCatching {
                    val json = context.resources.openRawResource(R.raw.fiches)
                        .bufferedReader().use { it.readText() }
                    gson.fromJson(json, FichesData::class.java)
                }.getOrNull() ?: FichesData()
            }.also { cache = it }
        }
    }

    /** Les thèmes dans l'ordre du fichier (chacun avec ses fiches). */
    suspend fun themes(): List<FicheTheme> = data().themes

    /** Les fiches d'un thème donné (nom officiel exact), vide si thème inconnu. */
    suspend fun fichesForTheme(theme: String): List<Fiche> =
        data().themes.firstOrNull { it.theme == theme }?.fiches ?: emptyList()

    /** Résout une fiche par son id unique, sur tout le dataset. */
    suspend fun fiche(id: String): Fiche? =
        data().themes.asSequence().flatMap { it.fiches.asSequence() }.firstOrNull { it.id == id }

    // --- Suivi de lecture des fiches (table read_fiche) ---

    /** Ids des fiches déjà consultées (pour la barre d'avancement par thème). */
    fun observeReadIds(): Flow<List<String>> = readFicheDao.observeReadIds()

    /** Marque une fiche comme consultée (idempotent). */
    suspend fun markRead(id: String) = readFicheDao.insert(ReadFiche(id))

    /** Nombre de fiches distinctes déjà consultées (progression des succès). */
    suspend fun readCount(): Int = readFicheDao.count()

    /** Nombre total de fiches sur tout le dataset (cible du succès « toutes les fiches »). */
    suspend fun totalFichesCount(): Int = data().themes.sumOf { it.fiches.size }

    /** Réinitialise le suivi de lecture (bouton « Réinitialiser la progression »). */
    suspend fun clearRead() = readFicheDao.clear()
}
