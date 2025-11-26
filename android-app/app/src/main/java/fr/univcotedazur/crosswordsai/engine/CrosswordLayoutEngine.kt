package fr.univcotedazur.crosswordsai.engine

import fr.univcotedazur.crosswordsai.data.model.BlackCell
import fr.univcotedazur.crosswordsai.data.model.CrosswordData
import fr.univcotedazur.crosswordsai.data.model.GridCell
import fr.univcotedazur.crosswordsai.data.model.WordDefinition

/**
 * Moteur de génération de grilles de mots croisés rectangulaires complètes.
 * Détecte automatiquement les mots en fonction des positions des cases noires.
 */
class CrosswordLayoutEngine {

    // Stocke les positions des mots : clé = "number_order_direction", valeur = Pair(x, y)
    private val wordPositions = mutableMapOf<String, Pair<Int, Int>>()
    private var gridWidth = 0
    private var gridHeight = 0

    /**
     * Génère une grille rectangulaire complète avec des cases noires et détecte automatiquement les mots
     */
    fun generateGrid(data: CrosswordData): List<GridCell> {
        gridWidth = data.width
        gridHeight = data.height
        wordPositions.clear()

        // Créer la grille complète avec toutes les cellules
        val grid = mutableListOf<GridCell>()
        val blackCellSet = data.blackCells.map { it.x to it.y }.toSet()

        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val isBlack = (x + 1 to y + 1) in blackCellSet
                grid.add(GridCell(
                    x = x,
                    y = y,
                    isEmpty = isBlack
                ))
            }
        }

        // Détection automatique des mots à partir de la grille
        val detectedWords = detectWords(blackCellSet)
        
        // Calculer les positions des mots détectés
        detectedWords.forEach { word ->
            val gridPosition = calculateGridPosition(word, blackCellSet)
            if (gridPosition != null) {
                wordPositions[word.idKey] = gridPosition
                println("🎯 Position grille pour ${word.idKey}: x=${gridPosition.first}, y=${gridPosition.second}")
            }
        }

        return grid
    }

    /**
     * Détecte tous les mots dans la grille en fonction des positions des cases noires
     * Un mot est toute séquence de 2 cellules blanches consécutives ou plus
     * order = ordre du mot sur cette ligne/colonne (1er, 2ème, 3ème...)
     */
    private fun detectWords(blackCellSet: Set<Pair<Int, Int>>): List<WordDefinition> {
        val words = mutableListOf<WordDefinition>()

        // Détection des mots horizontaux (parcourir chaque ligne)
        for (y in 1..gridHeight) {
            var wordOrder = 0  // Compteur pour l'ordre du mot
            var startX: Int? = null
            var length = 0

            for (x in 1..gridWidth) {
                val isBlack = (x to y) in blackCellSet
                
                if (!isBlack) {
                    // Cellule blanche
                    if (startX == null) {
                        startX = x
                    }
                    length++
                } else {
                    // Cellule noire - vérifier si on avait un mot avant
                    if (length >= 2 && startX != null) {
                        wordOrder++
                        words.add(WordDefinition(
                            number = y,
                            order = wordOrder,  // Ordre du mot sur cette ligne
                            size = length,
                            direction = "horizontal"
                        ))
                    }
                    startX = null
                    length = 0
                }
            }
            
            // Vérifier à la fin de la ligne
            if (length >= 2 && startX != null) {
                wordOrder++
                words.add(WordDefinition(
                    number = y,
                    order = wordOrder,  // Ordre du mot sur cette ligne
                    size = length,
                    direction = "horizontal"
                ))
            }
        }

        // Détection des mots verticaux (parcourir chaque colonne)
        for (x in 1..gridWidth) {
            var wordOrder = 0  // Compteur pour l'ordre du mot
            var startY: Int? = null
            var length = 0

            for (y in 1..gridHeight) {
                val isBlack = (x to y) in blackCellSet
                
                if (!isBlack) {
                    // Cellule blanche
                    if (startY == null) {
                        startY = y
                    }
                    length++
                } else {
                    // Cellule noire - vérifier si on avait un mot avant
                    if (length >= 2 && startY != null) {
                        wordOrder++
                        words.add(WordDefinition(
                            number = x,
                            order = wordOrder,  // Ordre du mot sur cette colonne
                            size = length,
                            direction = "vertical"
                        ))
                    }
                    startY = null
                    length = 0
                }
            }
            
            // Vérifier à la fin de la colonne
            if (length >= 2 && startY != null) {
                wordOrder++
                words.add(WordDefinition(
                    number = x,
                    order = wordOrder,  // Ordre du mot sur cette colonne
                    size = length,
                    direction = "vertical"
                ))
            }
        }

        return words
    }

    /**
     * Calcule la position réelle dans la grille (x, y) pour un mot en fonction de sa définition
     * order = ordre du mot (1er, 2ème, 3ème...) sur cette ligne/colonne
     * On doit trouver le Nième mot sur cette ligne/colonne
     */
    private fun calculateGridPosition(word: WordDefinition, blackCellSet: Set<Pair<Int, Int>>): Pair<Int, Int>? {
        return when (word.direction) {
            "horizontal" -> {
                // Parcourir la ligne pour trouver le Nième mot
                val y = word.number
                var wordOrder = 0
                var startX: Int? = null
                var length = 0

                for (x in 1..gridWidth) {
                    val isBlack = (x to y) in blackCellSet
                    
                    if (!isBlack) {
                        if (startX == null) startX = x
                        length++
                    } else {
                        if (length >= 2 && startX != null) {
                            wordOrder++
                            if (wordOrder == word.order) {
                                // Mot trouvé ! Retourner la position indexée à 0
                                return (startX - 1) to (y - 1)
                            }
                        }
                        startX = null
                        length = 0
                    }
                }
                
                // Vérifier à la fin de la ligne
                if (length >= 2 && startX != null) {
                    wordOrder++
                    if (wordOrder == word.order) {
                        return (startX - 1) to (y - 1)
                    }
                }
                null
            }
            "vertical" -> {
                // Parcourir la colonne pour trouver le Nième mot
                val x = word.number
                var wordOrder = 0
                var startY: Int? = null
                var length = 0

                for (y in 1..gridHeight) {
                    val isBlack = (x to y) in blackCellSet
                    
                    if (!isBlack) {
                        if (startY == null) startY = y
                        length++
                    } else {
                        if (length >= 2 && startY != null) {
                            wordOrder++
                            if (wordOrder == word.order) {
                                // Mot trouvé ! Retourner la position indexée à 0
                                return (x - 1) to (startY - 1)
                            }
                        }
                        startY = null
                        length = 0
                    }
                }
                
                // Vérifier à la fin de la colonne
                if (length >= 2 && startY != null) {
                    wordOrder++
                    if (wordOrder == word.order) {
                        return (x - 1) to (startY - 1)
                    }
                }
                null
            }
            else -> null
        }
    }

    /**
     * Récupère la position de départ d'un mot dans la grille
     * @param idKey L'identifiant unique du mot ("number_order_direction")
     * @return Pair(x, y) ou null si le mot n'existe pas
     */
    fun getWordPosition(idKey: String): Pair<Int, Int>? {
        return wordPositions[idKey]
    }

    /**
     * Obtient la liste de tous les mots détectés
     */
    fun getDetectedWords(data: CrosswordData): List<WordDefinition> {
        val blackCellSet = data.blackCells.map { it.x to it.y }.toSet()
        return detectWords(blackCellSet)
    }
}