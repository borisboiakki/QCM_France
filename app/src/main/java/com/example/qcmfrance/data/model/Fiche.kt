package com.example.qcmfrance.data.model

import androidx.annotation.Keep

/**
 * Fiches thématiques officielles embarquées pour la consultation **hors-ligne**.
 *
 * Contenu généré par `scripts/fetch_fiches.py` (pipeline GitHub Actions `update-fiches`) et bundlé
 * dans `res/raw/fiches.json`. Contrairement aux [Question], ces données ne sont **pas** persistées
 * en Room : elles sont relues du raw à chaque lancement (cf. [com.example.qcmfrance.data.repository.FichesRepository]),
 * donc le fichier bundlé est toujours la source de vérité — aucune migration ni versioning nécessaire.
 *
 * `@Keep` : ces modèles ne sont peuplés que par réflexion Gson ; l'annotation garantit que R8 ne
 * renomme/supprime pas leurs champs en build release (sinon le mapping clés JSON → champs casse).
 */
@Keep
data class FichesData(
    val version: Int = 0,
    val generatedAt: String = "",
    val source: String = "",
    val themes: List<FicheTheme> = emptyList()
)

/** Un des 5 thèmes officiels et l'ensemble de ses fiches (arborescence complète). */
@Keep
data class FicheTheme(
    val theme: String,          // nom officiel exact du thème
    val url: String,            // page d'index du thème sur le site officiel
    val fiches: List<Fiche> = emptyList()
)

/** Une fiche individuelle : contenu en markdown + lien source. */
@Keep
data class Fiche(
    val id: String,             // unique sur tout le dataset : "<slug-thème>__<slug-fiche>"
    val title: String,
    val url: String,            // page d'origine (bouton « Voir en ligne »)
    val markdown: String
)
