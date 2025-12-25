package fr.miage.m1.schemas

import kotlinx.serialization.Serializable

/**
 * Réponse d'erreur standard
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)
