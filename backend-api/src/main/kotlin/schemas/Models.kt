package fr.miage.m1.schemas

import kotlinx.serialization.Serializable

/**
 * Modèles généraux de l'application
 */

@Serializable
data class ErrorResponse(
    val error: String
)
