package fr.miage.m1.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Définition d'un mot dans la grille.
 * Unifie les modèles Android, Ktor et Solver.
 * 
 * Toutes les coordonnées sont 1-indexed.
 */
@Serializable
data class WordEntry(
    val number: Int,                 // Numéro de ligne ou colonne
    val order: Int = 1,              // Ordre sur la ligne/colonne (1er, 2ᵉ...)
    val direction: Direction,
    val size: Int,
    val start: Int,
    val end: Int,
    val clue: String = "",           // Définition (récupérée par OCR)

    // Remplis après résolution
    var answer: String? = null,
    @Transient var confidence: Int = 0
) {
    /** Identifiant unique du mot */
    val idKey: String get() = "${number}_${order}_${direction.name.lowercase()}"

    /**
     * Liste des positions occupées par ce mot (1-indexed)
     */
    val coordinates: List<Pair<Int, Int>>
        get() = (start..end).map { if (direction == Direction.HORIZONTAL) it to number else number to it }

    /**
     * Vérifie si le mot est résolu
     */
    fun isSolved(): Boolean = answer != null

    override fun toString(): String {
        val posInfo = if (direction == Direction.HORIZONTAL) " @($start,$number)" else " @($number,$start)"
        val answerInfo = answer?.let { " = $it" } ?: ""
        return "Word#$number-$order ${direction.name} ($size)$posInfo$answerInfo"
    }
}