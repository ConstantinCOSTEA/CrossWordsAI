package fr.miage.m1.crosswordsai.data.model

import kotlinx.serialization.Serializable

/**
 * Réponse contenant la liste des réponses aux mots
 */
@Serializable
data class WordAnswersResponse(
    val words: List<WordAnswer>
)

/**
 * Représente la réponse d'un mot
 */
@Serializable
data class WordAnswer(
    val number: Int,
    val order: Int,
    val direction: String,
    val answer: String
)