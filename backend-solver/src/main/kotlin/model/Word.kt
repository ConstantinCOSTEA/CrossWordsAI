package fr.univcotedazur.model

/**
 * Représente un mot dans la grille de mots croisés
 * 
 * @property number Numéro du mot
 * @property order Ordre (pour distinguer les mots avec le même numéro)
 * @property direction Direction du mot (HORIZONTAL ou VERTICAL)
 * @property definition Définition/indice du mot
 * @property cells Liste des cases occupées par ce mot
 * @property answer Réponse du mot (null si pas encore résolu)
 */
data class Word(
    val number: Int,
    val order: Int,
    val direction: Direction,
    val definition: String,
    val cells: List<Cell>,
    var answer: String? = null
) {
    /**
     * Retourne la longueur du mot
     */
    fun length(): Int = cells.size
    
    /**
     * Retourne les contraintes de position basées sur les lettres déjà remplies
     * dans les cases partagées avec d'autres mots
     * 
     * @return Map où la clé est l'index (0-based) et la valeur est le caractère connu
     */
    fun getConstraints(): Map<Int, Char> {
        return cells.mapIndexedNotNull { index, cell ->
            cell.char?.let { index to it }
        }.toMap()
    }
    
    /**
     * Remplit le mot avec la réponse donnée et met à jour les cases de la grille
     * 
     * @param answer La réponse à remplir
     * @throws IllegalArgumentException si la longueur ne correspond pas
     */
    fun fillAnswer(answer: String) {
        require(answer.length == cells.size) {
            "La longueur de la réponse (${answer.length}) ne correspond pas à la longueur du mot (${cells.size})"
        }
        
        this.answer = answer.uppercase()
        answer.uppercase().forEachIndexed { index, char ->
            cells[index].char = char
        }
    }
    
    /**
     * Vérifie si le mot est résolu
     */
    fun isSolved(): Boolean = answer != null
    
    /**
     * Tente de remplir automatiquement le mot si toutes ses cellules sont remplies
     * Cela permet de marquer comme "résolus" les mots qui ont été complétés par intersection
     * avec d'autres mots, sans avoir reçu de réponse directe de l'IA
     */
    fun tryAutoFill() {
        if (answer == null && cells.all { it.char != null }) {
            answer = cells.map { it.char }.joinToString("")
        }
    }
    
    override fun toString(): String {
        val constraintsStr = getConstraints().entries.joinToString(", ") { "${it.key + 1}=${it.value}" }
        val answerStr = answer ?: ("_".repeat(length()))
        return "Word($number-$order, ${direction.name}, $answerStr, def=\"$definition\", constraints=[$constraintsStr])"
    }
}
