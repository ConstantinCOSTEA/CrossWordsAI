package fr.univcotedazur.ai

import fr.univcotedazur.model.Word

/**
 * Constructeur de prompts enrichis pour l'IA basé sur les contraintes
 */
object ConstraintBuilder {

    /**
     * Construit un pattern visuel du mot avec les lettres connues
     * Exemple: _ O _ _ E _ pour un mot de 6 lettres avec O en position 2 et E en position 5
     */
    private fun buildWordPattern(length: Int, constraints: Map<Int, Char>): String {
        return (0 until length).joinToString(" ") { index ->
            constraints[index]?.toString() ?: "_"
        }
    }

    /**
     * Construit un prompt batch pour interroger l'IA sur plusieurs mots à la fois
     * 
     * @param words Liste des mots à résoudre
     * @return Un prompt formaté demandant toutes les réponses en une seule requête
     */
    fun buildBatchPrompt(words: List<Word>): String {
        val wordsList = words.mapIndexed { index, word ->
            val constraints = word.getConstraints()
            val constraintInfo = if (constraints.isNotEmpty()) {
                val pattern = buildWordPattern(word.length(), constraints)
                " | Pattern: $pattern"
            } else {
                ""
            }
            
            "${index + 1}. [${word.number}-${word.order}] \"${word.definition}\" (${word.length()} lettres)$constraintInfo"
        }.joinToString("\n")
        
        return """
            IMPORTANT: Tu dois trouver des mots FRANÇAIS pour TOUTES les définitions suivantes.
            
            DÉFINITIONS:
            $wordsList
            
            RÈGLES STRICTES:
            1. Tous les mots DOIVENT être en FRANÇAIS (pas d'anglais!)
            2. Chaque mot DOIT avoir EXACTEMENT le nombre de lettres indiqué
            3. Si un pattern est donné, le mot DOIT respecter ce pattern
            4. Les mots peuvent être au PLURIEL (ajout de S, X)
            5. Pour les noms propres géographiques: "São Paulo" → "SAOPAULO" (sans accents ni espaces)
            6. Pense aux spécificités géographiques et culturelles françaises
            
            FORMAT DE RÉPONSE (une ligne par mot):
            [ID] MOT CONFIANCE
            
            Où:
            - [ID] est le numéro de la définition (1, 2, 3...)
            - MOT est le mot en majuscules
            - CONFIANCE est ton niveau de certitude (0-100)
            
            Exemple:
            [1] ORDINATEUR 95
            [2] SOURIS 88
            [3] CLAVIER 92
        """.trimIndent()
    }
    
    /**
     * Parse la réponse batch de l'IA
     * Format attendu: "[1] MOT1 CONF1\n[2] MOT2 CONF2\n..."
     * 
     * @return Map de (index -> Pair(mot, confiance)) ou une map vide si erreur
     */
    fun parseBatchResponse(response: String, wordCount: Int): Map<Int, Pair<String, Int>> {
        val results = mutableMapOf<Int, Pair<String, Int>>()
        
        response.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            
            // Parse format: [N] MOT CONFIANCE
            val matchResult = Regex("\\[(\\d+)]\\s+(\\S+)\\s+(\\d+)").find(trimmed)
            if (matchResult != null) {
                val (indexStr, word, confidenceStr) = matchResult.destructured
                val index = indexStr.toIntOrNull()
                val confidence = confidenceStr.toIntOrNull()
                
                if (index != null && confidence != null && index in 1..wordCount && confidence in 0..100) {
                    results[index - 1] = word.uppercase() to confidence
                }
            }
        }
        
        return results
    }
}
