package fr.univcotedazur.model

/**
 * Représente une case de la grille de mots croisés
 * 
 * @property x Coordonnée X (colonne, 1-indexed)
 * @property y Coordonnée Y (ligne, 1-indexed)
 * @property char Caractère dans la case (null si vide)
 * @property isBlack Si la case est noire (bloquée)
 */
data class Cell(
    val x: Int,
    val y: Int,
    var char: Char? = null,
    val isBlack: Boolean = false
) {
    override fun toString(): String {
        return when {
            isBlack -> "#"
            char != null -> char.toString()
            else -> "."
        }
    }
}
