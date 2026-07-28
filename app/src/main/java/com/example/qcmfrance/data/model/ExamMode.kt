package com.example.qcmfrance.data.model

import com.example.qcmfrance.R

/**
 * Les trois QCM officiels couverts par l'application. Chacun a sa propre liste de questions de
 * **connaissances** ; les questions de **mise en situation** ([SHARED_CODE]) et les fiches
 * thématiques officielles sont communes aux trois.
 *
 * La mécanique d'examen est identique pour tous les modes (40 questions, 45 min, seuil 32/40,
 * répartition 28 connaissances + 12 mises en situation) — cf. `ExamConstants` et
 * `QuestionRepository`.
 *
 * @param code valeur persistée en base (colonne `examMode` de `questions`, `quiz_results`,
 *             `paused_quiz`) et préfixe des clés de cycle/progression. **Ne jamais renommer.**
 */
enum class ExamMode(
    val code: String,
    val labelRes: Int,
    val shortLabelRes: Int,
    val descriptionRes: Int
) {
    NATURALISATION(
        code = "NAT",
        labelRes = R.string.mode_nat_label,
        shortLabelRes = R.string.mode_nat_short,
        descriptionRes = R.string.mode_nat_desc
    ),
    RESIDENT_CARD(
        code = "CR",
        labelRes = R.string.mode_cr_label,
        shortLabelRes = R.string.mode_cr_short,
        descriptionRes = R.string.mode_cr_desc
    ),
    MULTI_YEAR_PERMIT(
        code = "CSP",
        labelRes = R.string.mode_csp_label,
        shortLabelRes = R.string.mode_csp_short,
        descriptionRes = R.string.mode_csp_desc
    );

    /**
     * Clé de cycle de tirage d'examen (table `exam_cycle`, PK = chaîne libre). Le mode
     * naturalisation conserve les clés historiques (`thème` / `thème::situation`) pour ne pas
     * réinitialiser le cycle des installations existantes.
     */
    fun cycleKey(theme: String, isSituation: Boolean): String {
        val base = if (this == NATURALISATION) theme else "$code::$theme"
        return if (isSituation) "$base::situation" else base
    }

    /**
     * Clé d'avancement d'entraînement (table `training_progress`, PK = chaîne libre). Comme pour
     * [cycleKey], la naturalisation garde la clé nue : la progression déjà enregistrée par les
     * utilisateurs reste valide après mise à jour.
     */
    fun trainingKey(theme: String): String =
        if (this == NATURALISATION) theme else "$code::$theme"

    companion object {
        /**
         * Code des questions communes à tous les modes (mises en situation) : elles sont tirées
         * quel que soit le QCM sélectionné.
         */
        const val SHARED_CODE = "ALL"

        val DEFAULT: ExamMode = NATURALISATION

        fun fromCode(code: String?): ExamMode =
            entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}
