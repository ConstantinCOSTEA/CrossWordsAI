package fr.miage.m1.model

import kotlinx.serialization.Serializable

/**
 * Position d'une case noire dans la grille (1-indexed)
 */
@Serializable
data class BlackCell(
    val x: Int,  // Colonne (1-indexed)
    val y: Int   // Ligne (1-indexed)
)
