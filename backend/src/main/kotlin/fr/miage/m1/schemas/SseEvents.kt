package fr.miage.m1.schemas

import kotlinx.serialization.Serializable

/**
 * Événement SSE pour un round terminé
 */
@Serializable
data class RoundResultEvent(
    val round: Int,
    val solved: Int,
    val total: Int,
    val words: List<WordAnswer>
)

/**
 * Événement SSE final
 */
@Serializable
data class FinalResultEvent(
    val rounds: Int,
    val solved: Int,
    val total: Int,
    val words: List<WordAnswer>
)
