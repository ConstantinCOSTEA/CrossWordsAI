package fr.miage.m1.schemas

import fr.miage.m1.model.BlackCell
import fr.miage.m1.model.Direction
import kotlinx.serialization.Serializable

/**
 * Réponse de l'analyse de grille (envoyée à Android)
 * Compatible avec le format crossword_grid_test.json
 */
@Serializable
data class GridResponse(
    val height: Int,
    val width: Int,
    val x: String = "letters",
    val y: String = "numbers",
    val blackCells: List<BlackCell>,
    val words: List<WordInfo>
)

/**
 * Information sur un mot (sans position interne, juste ce que Android a besoin)
 */
@Serializable
data class WordInfo(
    val number: Int,
    val order: Int,
    val direction: Direction,
    val size: Int,
    val clue: String
)
