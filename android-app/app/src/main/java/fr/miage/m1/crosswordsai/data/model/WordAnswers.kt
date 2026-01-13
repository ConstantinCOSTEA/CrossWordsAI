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

/**
 * Événement SSE pour un round terminé (reçu de /solve-stream)
 */
@Serializable
data class RoundResultEvent(
    val round: Int,
    val solved: Int,
    val total: Int,
    val words: List<WordAnswer>
)

/**
 * Événement SSE final (reçu de /solve-stream)
 */
@Serializable
data class FinalResultEvent(
    val rounds: Int,
    val solved: Int,
    val total: Int,
    val words: List<WordAnswer>
)

/**
 * Sealed class pour représenter les différents types d'événements SSE
 */
sealed class SseEvent {
    data class Connected(val status: String) : SseEvent()
    data class Round(val event: RoundResultEvent) : SseEvent()
    data class Complete(val event: FinalResultEvent) : SseEvent()
    data class Error(val message: String) : SseEvent()
}
