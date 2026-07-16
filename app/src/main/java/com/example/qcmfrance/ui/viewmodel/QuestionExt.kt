package com.example.qcmfrance.ui.viewmodel

import com.example.qcmfrance.data.model.Question

/**
 * Choisit au hasard un jeu de réponses parmi { question de base } ∪ [Question.variants] et renvoie
 * une copie matérialisée (options + [Question.correctAnswer] du jeu retenu, `variants` vidées).
 * L'id ne change pas : une question à variantes reste tirée une seule fois par examen ; c'est
 * seulement le jeu de réponses affiché qui tourne d'un tirage à l'autre. À appeler **avant**
 * [withShuffledOptions]. Partagé par l'examen ([QuizViewModel]) et l'entraînement ([TrainingViewModel]).
 */
internal fun Question.pickVariant(): Question {
    if (variants.isEmpty()) return this
    val sets = buildList {
        add(this@pickVariant.copy(variants = emptyList()))
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
    return sets.random()
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
