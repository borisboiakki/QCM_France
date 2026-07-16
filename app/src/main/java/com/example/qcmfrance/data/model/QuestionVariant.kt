package com.example.qcmfrance.data.model

/**
 * Jeu de réponses alternatif pour une [Question] admettant plusieurs bonnes réponses valides
 * (ex. « Quel musée est situé à Paris ? » → Louvre, mais aussi Orsay, Pompidou…).
 *
 * Une variante = 4 options + la lettre de la bonne réponse, exactement comme une question ; seul
 * le jeu de réponses change, l'énoncé ([Question.text]) et la [Question.source] restent ceux de la
 * question de base. Ce n'est **pas** une entité Room : les variantes sont stockées sérialisées dans
 * la colonne `variants` de la table `questions` (cf. [com.example.qcmfrance.data.db.Converters]),
 * donc rattachées à l'id de la question de base — une question à variantes n'est jamais tirée plus
 * d'une fois par examen.
 */
data class QuestionVariant(
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String   // "A", "B", "C" ou "D"
)
