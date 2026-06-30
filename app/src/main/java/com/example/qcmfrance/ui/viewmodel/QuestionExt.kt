package com.example.qcmfrance.ui.viewmodel

import com.example.qcmfrance.data.model.Question

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
