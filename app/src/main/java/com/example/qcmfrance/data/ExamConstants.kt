package com.example.qcmfrance.data

/** Règles officielles de l'examen civique — source unique pour toute l'app. */
object ExamConstants {
    /** Durée maximale de l'examen : 45 minutes. */
    const val EXAM_DURATION_SECONDS = 45 * 60

    /** Score minimal pour réussir : 32/40 (80 %). */
    const val PASS_THRESHOLD = 32

    /** Seuil sous lequel le chronomètre passe en rouge : 5 minutes. */
    const val TIMER_WARNING_SECONDS = 5 * 60
}
