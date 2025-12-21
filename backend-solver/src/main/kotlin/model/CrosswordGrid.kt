package fr.univcotedazur.model

/**
 * Représente la grille complète de mots croisés
 * 
 * @property width Largeur de la grille
 * @property height Hauteur de la grille
 * @property grid Grille 2D de cases
 * @property words Liste de tous les mots dans la grille
 */
class CrosswordGrid(
    val width: Int,
    val height: Int,
    private val blackCells: Set<Pair<Int, Int>>,
    initialWords: List<Word>
) {
    private val grid: Array<Array<Cell>> = Array(height) { y ->
        Array(width) { x ->
            // Coordonnées 1-indexed
            val isBlack = blackCells.contains(Pair(x + 1, y + 1))
            Cell(x + 1, y + 1, isBlack = isBlack)
        }
    }
    
    private var _words: List<Word> = initialWords
    val words: List<Word> get() = _words
    
    /**
     * Définit la liste des mots (appelé après construction pour lier les mots aux cellules)
     */
    fun setWords(words: List<Word>) {
        this._words = words
    }
    
    /**
     * Obtient une case à la position donnée (1-indexed)
     */
    fun getCell(x: Int, y: Int): Cell {
        require(x in 1..width && y in 1..height) {
            "Coordonnées hors limites: ($x, $y)"
        }
        return grid[y - 1][x - 1]
    }
    
    /**
     * Remplit un mot avec la réponse donnée
     * La propagation est automatique car les cases sont partagées
     */
    fun fillWord(word: Word, answer: String) {
        word.fillAnswer(answer)
    }
    
    /**
     * Retourne le nombre de mots résolus
     */
    fun getSolvedCount(): Int = words.count { it.isSolved() }
    
    /**
     * Retourne le nombre total de mots
     */
    fun getTotalCount(): Int = words.size
    
    /**
     * Détecte les conflits entre les mots qui se croisent
     * Un conflit se produit quand deux mots résolus partagent une case mais ont des lettres différentes
     * 
     * @return Liste de paires de mots en conflit
     */
    fun detectConflicts(): List<Pair<Word, Word>> {
        val conflicts = mutableListOf<Pair<Word, Word>>()
        val solvedWords = words.filter { it.isSolved() }
        
        // Comparer chaque paire de mots résolus
        for (i in solvedWords.indices) {
            for (j in i + 1 until solvedWords.size) {
                val word1 = solvedWords[i]
                val word2 = solvedWords[j]
                
                // Trouver les cases partagées
                val sharedCells = word1.cells.intersect(word2.cells.toSet())
                
                // Vérifier si les lettres correspondent
                for (cell in sharedCells) {
                    val index1 = word1.cells.indexOf(cell)
                    val index2 = word2.cells.indexOf(cell)
                    
                    val char1 = word1.answer?.getOrNull(index1)
                    val char2 = word2.answer?.getOrNull(index2)
                    
                    if (char1 != null && char2 != null && char1 != char2) {
                        // Conflit trouvé!
                        conflicts.add(word1 to word2)
                        break // Pas besoin de vérifier les autres cases pour cette paire
                    }
                }
            }
        }
        
        return conflicts
    }
    
    /**
     * Enlève un mot de la grille (efface sa réponse et vide les cases)
     * 
     * @param word Le mot à enlever
     */
    fun removeWord(word: Word) {
        // Effacer seulement les cases qui n'appartiennent qu'à ce mot
        for (cell in word.cells) {
            // Vérifier si la case est partagée avec un autre mot résolu
            val otherSolvedWords = words.filter { 
                it != word && it.isSolved() && it.cells.contains(cell)
            }
            
            // Si aucun autre mot résolu utilise cette case, on peut l'effacer
            if (otherSolvedWords.isEmpty()) {
                cell.char = null
            }
        }
        
        // Réinitialiser la réponse du mot
        word.answer = null
    }
    
    /**
     * Affiche la grille sous forme de texte
     */
    fun displayGrid(): String {
        val sb = StringBuilder()
        
        // En-tête avec numéros de colonnes
        sb.append("    ")
        for (x in 1..width) {
            sb.append("$x ")
        }
        sb.append("\n")
        sb.append("   ").append("-".repeat(width * 2 + 1)).append("\n")
        
        // Lignes de la grille
        for (y in 1..height) {
            sb.append(String.format("%2d |", y))
            for (x in 1..width) {
                sb.append(getCell(x, y).toString()).append(" ")
            }
            sb.append("\n")
        }
        
        return sb.toString()
    }
    
    override fun toString(): String {
        return "CrosswordGrid(${width}x${height}, ${getSolvedCount()}/${getTotalCount()} mots résolus)"
    }
}
