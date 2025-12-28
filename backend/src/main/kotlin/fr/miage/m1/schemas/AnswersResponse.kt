package fr.miage.m1.schemas

import fr.miage.m1.model.Direction
import kotlinx.serialization.Serializable

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
