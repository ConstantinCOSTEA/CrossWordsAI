package fr.univcotedazur.crosswordsai.engine

import fr.univcotedazur.crosswordsai.data.model.CrosswordData
import fr.univcotedazur.crosswordsai.data.model.GridCell
import fr.univcotedazur.crosswordsai.data.model.WordDefinition
import kotlin.math.max
import kotlin.math.min

class CrosswordLayoutEngine {

    // Map pour stocker la position de départ. La clé est l'ID unique: "number_direction"
    private val placedWords = mutableMapOf<String, Pair<Int, Int>>()
    // Map des mots, la clé est le numéro (Int). Ceci est une liste pour gérer les doublons.
    private lateinit var wordsByNumber: Map<Int, List<WordDefinition>>

    private var offsetX = 0
    private var offsetY = 0

    fun generateGrid(data: CrosswordData): List<GridCell> {
        wordsByNumber = data.words.groupBy { it.number }
        placedWords.clear()

        // 1. Placement récursif
        if (data.words.isNotEmpty()) {
            val firstWord = data.words.first()
            placeWordRecursively(firstWord, 0, 0)
        }

        if (placedWords.isEmpty()) return emptyList()

        // 2. Calcul des limites (Bounding Box)
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE

        placedWords.forEach { (idKey, startPos) ->
            val word = getWordFromIdKey(idKey) ?: return@forEach

            val (sx, sy) = startPos
            val dx = if (word.direction == "horizontal") 1 else 0
            val dy = if (word.direction == "vertical") 1 else 0

            minX = min(minX, sx)
            minY = min(minY, sy)
            maxX = max(maxX, sx + (word.size - 1) * dx)
            maxY = max(maxY, sy + (word.size - 1) * dy)
        }

        // Stocker l'offset pour la méthode getWordPosition
        offsetX = minX
        offsetY = minY

        // 3. Création des cases (uniquement les cases nécessaires)
        val result = mutableListOf<GridCell>()

        placedWords.forEach { (idKey, startPos) ->
            val word = getWordFromIdKey(idKey)!!
            val startX = startPos.first - minX
            val startY = startPos.second - minY
            val dx = if (word.direction == "horizontal") 1 else 0
            val dy = if (word.direction == "vertical") 1 else 0

            for (i in 0 until word.size) {
                val cx = startX + (i * dx)
                val cy = startY + (i * dy)
                val cellNum = if (i == 0) word.number else null

                // Vérifier si une case existe déjà à cette position
                val existingCell = result.find { it.x == cx && it.y == cy }

                if (existingCell == null) {
                    result.add(GridCell(x = cx, y = cy, number = cellNum, isEmpty = false))
                } else if (cellNum != null && existingCell.number == null) {
                    // Mise à jour du numéro si nécessaire (pour les intersections)
                    result.remove(existingCell)
                    result.add(existingCell.copy(number = cellNum))
                }
            }
        }

        return result
    }

    /**
     * Récupère la position normalisée d'un mot dans la grille finale
     * @param idKey L'identifiant unique du mot ("number_direction")
     * @return Pair(x, y) ou null si le mot n'existe pas
     */
    fun getWordPosition(idKey: String): Pair<Int, Int>? {
        val rawPosition = placedWords[idKey] ?: return null
        return (rawPosition.first - offsetX) to (rawPosition.second - offsetY)
    }

    /**
     * Tente de récupérer un mot basé sur sa clé unique ("number_direction").
     */
    private fun getWordFromIdKey(idKey: String): WordDefinition? {
        val parts = idKey.split("_")
        val number = parts.firstOrNull()?.toIntOrNull() ?: return null
        val direction = parts.getOrNull(1) ?: return null

        return wordsByNumber[number]?.find { it.direction == direction }
    }

    /**
     * Fonction récursive principale pour placer un mot.
     */
    private fun placeWordRecursively(
        currentWord: WordDefinition,
        startX: Int,
        startY: Int
    ) {
        // Utilise directement la propriété idKey de WordDefinition
        if (placedWords.containsKey(currentWord.idKey)) return

        placedWords[currentWord.idKey] = startX to startY

        currentWord.crossings.forEach { crossing ->

            // 1. Déduire la direction requise pour le mot croisé
            val requiredDirection = if (currentWord.direction == "horizontal") "vertical" else "horizontal"

            // 2. Chercher le mot croisé correspondant (uniquement ceux qui ont la direction requise)
            val potentialOtherWords = wordsByNumber[crossing.crossingWordNumber]
            val otherWord = potentialOtherWords?.find { it.direction == requiredDirection }

            if (otherWord != null) {
                if (placedWords.containsKey(otherWord.idKey)) return@forEach

                // Logique de placement
                val currentIdx = crossing.position - 1
                val dx = if (currentWord.direction == "horizontal") 1 else 0
                val dy = if (currentWord.direction == "vertical") 1 else 0
                val intersectX = startX + (currentIdx * dx)
                val intersectY = startY + (currentIdx * dy)

                val otherCrossingRef = otherWord.crossings.find { it.crossingWordNumber == currentWord.number }

                if (otherCrossingRef != null) {
                    val otherIdx = otherCrossingRef.position - 1
                    val odx = if (otherWord.direction == "horizontal") 1 else 0
                    val ody = if (otherWord.direction == "vertical") 1 else 0

                    val otherStartX = intersectX - (otherIdx * odx)
                    val otherStartY = intersectY - (otherIdx * ody)

                    // Appel récursif avec le nouveau mot
                    placeWordRecursively(otherWord, otherStartX, otherStartY)
                }
            }
        }
    }
}