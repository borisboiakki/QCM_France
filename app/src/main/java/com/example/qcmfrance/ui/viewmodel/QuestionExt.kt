package com.example.qcmfrance.ui.viewmodel

import com.example.qcmfrance.data.model.Question

/**
 * Tous les jeux de réponses d'une question : { question de base } ∪ [Question.variants], chacun
 * matérialisé dans une copie (options + [Question.correctAnswer] du jeu, `variants` vidées pour
 * éviter toute ré-expansion). L'id est le même pour toutes les copies. À appeler **avant**
 * [withShuffledOptions]. Utilisé tel quel par l'entraînement ([TrainingViewModel]), qui déroule
 * tous les jeux séquentiellement, et via [pickVariant] par l'examen.
 */
internal fun Question.allAnswerSets(): List<Question> {
    if (variants.isEmpty()) return listOf(this)
    return buildList {
        add(this@allAnswerSets.copy(variants = emptyList()))
        addAll(variants.map { v ->
            copy(
                optionA = v.optionA,
                optionB = v.optionB,
                optionC = v.optionC,
                optionD = v.optionD,
                correctAnswer = v.correctAnswer,
                variants = emptyList()
            )
        })
    }
}

/**
 * Choisit au hasard un jeu de réponses parmi [allAnswerSets] et renvoie sa copie matérialisée.
 * L'id ne change pas : une question à variantes reste tirée une seule fois par examen ; c'est
 * seulement le jeu de réponses affiché qui tourne d'un tirage à l'autre. Utilisé par l'examen
 * ([QuizViewModel]) uniquement — l'entraînement montre tous les jeux via [allAnswerSets].
 */
internal fun Question.pickVariant(): Question {
    if (variants.isEmpty()) return this
    return allAnswerSets().random()
}

/**
 * Mélange aléatoirement les 4 options d'une question et met à jour [Question.correctAnswer]
 * en conséquence, pour que la bonne réponse ne soit jamais toujours à la même position.
 * Partagé par le mode examen ([QuizViewModel]) et le mode entraînement ([TrainingViewModel]).
 */
internal fun Question.withShuffledOptions(): Question {
    val letters = listOf("A", "B", "C", "D")
    val originals = listOf(optionA, optionB, optionC, optionD)
    val shuffledIndices = (0..3).shuffled()
    val newOptions = shuffledIndices.map { originals[it] }
    val origCorrectIdx = letters.indexOf(correctAnswer)
    val newCorrectIdx = shuffledIndices.indexOf(origCorrectIdx)
    return copy(
        optionA = newOptions[0],
        optionB = newOptions[1],
        optionC = newOptions[2],
        optionD = newOptions[3],
        correctAnswer = letters[newCorrectIdx]
    )
}
