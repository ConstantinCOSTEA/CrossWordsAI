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
    val number: Int,                 // Numéro de ligne (H) ou colonne (V)
    val order: Int = 1,              // Ordre sur la ligne/colonne (1er, 2ᵉ...)
    val direction: Direction,
    val size: Int,                   // Longueur du mot
    val clue: String = "",           // Définition (récupérée par OCR)
    
    // Position de départ (calculée par le backend)
    val startX: Int = 0,             // Colonne de départ (1-indexed)
    val startY: Int = 0,             // Ligne de départ (1-indexed)
    
    // Remplis après résolution
    var answer: String? = null,
    @Transient var confidence: Int = 0
) {
    /** Identifiant unique du mot */
    val idKey: String get() = "${number}_${order}_${direction.name.lowercase()}"
    
    /** Coordonnée X de fin */
    val endX: Int get() = if (direction == Direction.HORIZONTAL) startX + size - 1 else startX
    
    /** Coordonnée Y de fin */
    val endY: Int get() = if (direction == Direction.VERTICAL) startY + size - 1 else startY
    
    /**
     * Liste des positions occupées par ce mot (1-indexed)
     */
    fun getCellPositions(): List<Pair<Int, Int>> {
        return if (direction == Direction.HORIZONTAL) {
            (startX..endX).map { x -> Pair(x, startY) }
        } else {
            (startY..endY).map { y -> Pair(startX, y) }
        }
    }
    
    /**
     * Vérifie si le mot est résolu
     */
    fun isSolved(): Boolean = answer != null
    
    override fun toString(): String {
        val posInfo = if (startX > 0 || startY > 0) " @($startX,$startY)" else ""
        val answerInfo = answer?.let { " = $it" } ?: ""
        return "Word#$number-$order ${direction.name} ($size)$posInfo$answerInfo"
    }
}
