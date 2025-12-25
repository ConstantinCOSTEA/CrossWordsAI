package fr.miage.m1.crosswordsai.data.model

import kotlinx.serialization.Serializable

/**
 * Représente les données complètes de la grille de mots croisés en mode plein (rectangulaire)
 */
@Serializable
data class CrosswordData(
    val height: Int,
    val width: Int,
    val x: String, // "numbers" ou "letters"
    val y: String, // "numbers" ou "letters"
    val blackCells: List<BlackCell> = emptyList(),
    val words: List<WordDefinition> = emptyList()
)

/**
 * Représente la position d'une case noire dans la grille
 */
@Serializable
data class BlackCell(
    val x: Int,
    val y: Int
)

/**
 * Représente une définition de mot dans le mode grille complète
 * @param number Le numéro du mot (correspond à la valeur de l'axe : A=1, B=2, etc. pour les lettres, ou numéro direct)
 * @param order L'ordre du mot sur sa ligne/colonne (1er, 2ème, 3ème...)
 * @param size La longueur du mot
 * @param direction La direction du mot : "horizontal" ou "vertical"
 */
@Serializable
data class WordDefinition(
    val number: Int,
    val order: Int,
    val size: Int,
    val direction: String,
    val clue: String = ""  // Définition du mot (récupérée par OCR)
) {
    val idKey: String
        get() = "${number}_${order}_$direction"
}

/**
 * Représente une cellule dans la grille de mots croisés pour le rendu UI
 */
data class GridCell(
    val x: Int,
    val y: Int,
    val number: Int? = null,
    val char: Char? = null,
    val isEmpty: Boolean = false
)