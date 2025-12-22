package fr.miage.m1.parser

import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.Direction
import fr.miage.m1.model.Word
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser pour convertir le JSON de mots croisés en CrosswordGrid
 */
object CrosswordJsonParser {
    
    @Serializable
    private data class BlackCellJson(val x: Int, val y: Int)
    
    @Serializable
    private data class WordJson(
        val number: Int,
        val order: Int,
        val direction: String,
        val definition: String,
        val start: Int,
        val end: Int
    )
    
    @Serializable
    private data class CrosswordJson(
        val height: Int,
        val width: Int,
        val blackCells: List<BlackCellJson>,
        val words: List<WordJson>
    )
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Parse un JSON de mots croisés et crée une CrosswordGrid
     * 
     * Le JSON doit avoir le format:
     * - height, width: dimensions de la grille
     * - blackCells: liste des cases noires avec coordonnées x, y (1-indexed)
     * - words: liste des mots avec:
     *   - number, order: identifiants du mot
     *   - direction: "horizontal" ou "vertical"
     *   - definition: définition du mot
     *   - start, end: positions de début et fin sur l'axe (1-indexed)
     * 
     * Pour les mots horizontaux: start/end sont les colonnes X
     * Pour les mots verticaux: start/end sont les lignes Y
     * La position sur l'autre axe est déduite de l'ordre d'apparition
     */
    fun parse(jsonString: String): CrosswordGrid {
        val crosswordJson = json.decodeFromString<CrosswordJson>(jsonString)
        
        // Convertir les cases noires en Set de pairs
        val blackCells = crosswordJson.blackCells.map { Pair(it.x, it.y) }.toSet()
        
        // Grouper les mots par direction pour calculer leurs positions
        val horizontalWords = crosswordJson.words.filter { it.direction == "horizontal" }
        val verticalWords = crosswordJson.words.filter { it.direction == "vertical" }
        
        // Calculer les positions Y pour les mots horizontaux (ordre d'apparition = ligne)
        val horizontalWordsWithY = calculateHorizontalWordPositions(horizontalWords)
        
        // Calculer les positions X pour les mots verticaux (numéro + ordre = colonne)
        val verticalWordsWithX = calculateVerticalWordPositions(verticalWords)
        
        // D'abord créer la grille avec toutes les cases
        val grid = CrosswordGrid(
            width = crosswordJson.width,
            height = crosswordJson.height,
            blackCells = blackCells,
            initialWords = emptyList() // Temporairement vide
        )
        
        // Créer les objets Word en référençant les cases de la grille
        val words = mutableListOf<Word>()
        
        // Traiter les mots horizontaux
        horizontalWordsWithY.forEach { (wordJson, y) ->
            val cells = (wordJson.start..wordJson.end).map { x ->
                grid.getCell(x, y) // Référencer la cellule existante de la grille!
            }
            
            words.add(
                Word(
                    number = wordJson.number,
                    order = wordJson.order,
                    direction = Direction.HORIZONTAL,
                    definition = wordJson.definition,
                    cells = cells
                )
            )
        }
        
        // Traiter les mots verticaux
        verticalWordsWithX.forEach { (wordJson, x) ->
            val cells = (wordJson.start..wordJson.end).map { y ->
                grid.getCell(x, y) // Référencer la cellule existante de la grille!
            }
            
            words.add(
                Word(
                    number = wordJson.number,
                    order = wordJson.order,
                    direction = Direction.VERTICAL,
                    definition = wordJson.definition,
                    cells = cells
                )
            )
        }
        
        // Maintenant lier les mots à la grille
        grid.setWords(words)
        
        return grid
    }
    
    /**
     * Calcule les positions Y (lignes) pour les mots horizontaux
     * Le numéro du mot correspond à la ligne Y
     */
    private fun calculateHorizontalWordPositions(horizontalWords: List<WordJson>): List<Pair<WordJson, Int>> {
        return horizontalWords.map { word ->
            val y = word.number // Le numéro du mot correspond à la ligne
            word to y
        }
    }
    
    /**
     * Calcule les positions X (colonnes) pour les mots verticaux
     * basé sur leur numéro (qui correspond à la colonne)
     */
    private fun calculateVerticalWordPositions(verticalWords: List<WordJson>): List<Pair<WordJson, Int>> {
        return verticalWords.map { word ->
            val x = word.number // Le numéro du mot correspond à la colonne
            word to x
        }
    }
}
