package fr.miage.m1.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Représente une grille de mots croisés complète.
 * Format compatible avec Android.
 * 
 * Toutes les coordonnées sont 1-indexed.
 */
@Serializable
data class CrosswordGrid(
    val height: Int,                              // Nombre de lignes
    val width: Int,                               // Nombre de colonnes
    val x: String = "letters",                    // Type d'axe X ("letters" ou "numbers")
    val y: String = "numbers",                    // Type d'axe Y
    val blackCells: List<BlackCell> = emptyList(),
    val words: MutableList<WordEntry> = mutableListOf()
) {
    // Grille interne pour le solver (pas sérialisée)
    @Transient
    private val cells: Array<Array<Char?>> = Array(height) { arrayOfNulls(width) }
    
    /**
     * Récupère le caractère à la position (1-indexed)
     */
    fun getChar(x: Int, y: Int): Char? {
        if (x !in 1..width || y !in 1..height) return null
        return cells[y - 1][x - 1]
    }
    
    /**
     * Place un caractère à la position (1-indexed)
     */
    fun setChar(x: Int, y: Int, char: Char?) {
        if (x in 1..width && y in 1..height) {
            cells[y - 1][x - 1] = char
        }
    }
    
    /**
     * Vérifie si une case est noire
     */
    fun isBlackCell(x: Int, y: Int): Boolean {
        return blackCells.any { it.x == x && it.y == y }
    }
    
    /**
     * Récupère les contraintes pour un mot (lettres déjà placées)
     * @return Map index (0-based) -> caractère
     */
    fun getConstraints(word: WordEntry): Map<Int, Char> {
        val constraints = mutableMapOf<Int, Char>()
        word.coordinates.forEachIndexed { index, (x, y) ->
            getChar(x, y)?.let { constraints[index] = it }
        }
        return constraints
    }
    
    /**
     * Place un mot dans la grille
     */
    fun fillWord(word: WordEntry, answer: String) {
        require(answer.length == word.size) {
            "Longueur incorrecte pour ${word.idKey}: attendu ${word.size}, recu ${answer.length}"
        }
        word.answer = answer.uppercase()
        word.coordinates.forEachIndexed { index, (x, y) ->
            setChar(x, y, answer[index].uppercaseChar())
        }
    }
    
    /**
     * Enlève un mot de la grille
     */
    fun removeWord(word: WordEntry) {
        word.coordinates.forEach { (x, y) ->
            // Ne pas effacer si un autre mot résolu utilise cette case
            val otherWords = words.filter { 
                it != word && it.isSolved() && it.coordinates.contains(Pair(x, y))
            }
            if (otherWords.isEmpty()) {
                setChar(x, y, null)
            }
        }
        word.answer = null
    }
    
    /**
     * Tente de compléter automatiquement un mot si toutes ses cases sont remplies
     */
    fun tryAutoFillWord(word: WordEntry) {
        if (word.answer == null) {
            val chars = word.coordinates.map { (x, y) -> getChar(x, y) }
            if (chars.all { it != null }) {
                word.answer = chars.joinToString("") { it.toString() }
            }
        }
    }
    
    /**
     * Détecte les conflits entre mots résolus
     * @return Liste de paires de mots en conflit
     */
    fun detectConflicts(): List<Pair<WordEntry, WordEntry>> {
        val conflicts = mutableListOf<Pair<WordEntry, WordEntry>>()
        val solvedWords = words.filter { it.isSolved() }
        
        for (i in solvedWords.indices) {
            for (j in i + 1 until solvedWords.size) {
                val word1 = solvedWords[i]
                val word2 = solvedWords[j]
                
                val sharedCells = word1.coordinates.intersect(word2.coordinates.toSet())
                
                for ((x, y) in sharedCells) {
                    val idx1 = word1.coordinates.indexOf(Pair(x, y))
                    val idx2 = word2.coordinates.indexOf(Pair(x, y))
                    
                    val char1 = word1.answer?.getOrNull(idx1)
                    val char2 = word2.answer?.getOrNull(idx2)
                    
                    if (char1 != null && char2 != null && char1 != char2) {
                        conflicts.add(word1 to word2)
                        break
                    }
                }
            }
        }
        
        return conflicts
    }
    
    fun getSolvedCount(): Int = words.count { it.isSolved() }
    fun getTotalCount(): Int = words.size

    /**
     * Affichage texte de la grille (debug)
     */
    fun display(): String {
        val sb = StringBuilder()
        
        // En-tête colonnes
        sb.append("    ")
        for (col in 1..width) sb.append("${col % 10} ")
        sb.append("\n   ").append("-".repeat(width * 2 + 1)).append("\n")
        
        // Lignes
        for (row in 1..height) {
            sb.append("${row.toString().padStart(2)} |")
            for (col in 1..width) {
                val char = when {
                    isBlackCell(col, row) -> "#"
                    getChar(col, row) != null -> getChar(col, row).toString()
                    else -> "."
                }
                sb.append("$char ")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
    
    companion object {
        /**
         * Crée une grille vide avec les dimensions données
         */
        fun create(
            width: Int,
            height: Int,
            blackCells: List<BlackCell> = emptyList(),
            xAxis: String = "letters",
            yAxis: String = "numbers"
        ): CrosswordGrid {
            return CrosswordGrid(
                width = width,
                height = height,
                x = xAxis,
                y = yAxis,
                blackCells = blackCells,
                words = mutableListOf()
            )
        }
    }
}
