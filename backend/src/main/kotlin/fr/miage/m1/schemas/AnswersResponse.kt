package fr.miage.m1.schemas

import fr.miage.m1.model.Direction
import kotlinx.serialization.Serializable

/**
 * Réponse de la résolution (envoyée à Android)
 * Compatible avec le format crossword_answers_test.json
 */
@Serializable
data class AnswersResponse(
    val words: List<WordAnswer>
)

/**
 * Réponse pour un mot résolu
 */
@Serializable
data class WordAnswer(
    val number: Int,
    val order: Int,
    val direction: Direction,
    val answer: String
)
