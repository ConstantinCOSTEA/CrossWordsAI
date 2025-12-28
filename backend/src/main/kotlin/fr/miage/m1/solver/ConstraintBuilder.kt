package fr.miage.m1.solver

import fr.miage.m1.model.WordEntry

object ConstraintBuilder {

    val SYSTEM_PROMPT = """
        Tu es un expert en mots croisés. Ta mission est de trouver des mots qui correspondent aux définitions.
        
        RÈGLES IMPÉRATIVES :
        1. Langue : FRANÇAIS uniquement.
        2. Longueur : Respecte strictement le nombre de lettres attendu.
        3. Formatage : Envoie les réponses en MAJUSCULES, SANS accents, SANS espaces (ex: "SAO PAULO" -> "SAOPAULO").
        4. Contraintes : Si un pattern est fourni (ex: _ A _ E), tu DOIS le respecter.
        
        FORMAT DE SORTIE ATTENDU (une ligne par réponse) :
        [ID] MOT CONFIANCE
        
        Exemple :
        [1] ORDINATEUR 95
        [2] POMME 100
    """.trimIndent()

    private fun buildWordPattern(length: Int, constraints: Map<Int, Char>): String {
        return (0 until length).joinToString(" ") { index ->
            constraints[index]?.toString() ?: "_"
        }
    }

    /**
     * Le prompt utilisateur est maintenant beaucoup plus léger.
     * Il ne contient que la liste des tâches à effectuer.
     */
    fun buildBatchPrompt(words: List<Pair<WordEntry, Map<Int, Char>>>): String {
        val wordsList = words.mapIndexed { index, (word, constraints) ->
            val constraintInfo = if (constraints.isNotEmpty()) {
                " | Pattern imposé: ${buildWordPattern(word.size, constraints)}"
            } else {
                ""
            }
            // On simplifie la ligne de définition
            "[${index + 1}] (${word.size} lettres) : \"${word.clue}\"$constraintInfo"
        }.joinToString("\n")

        return """
            Voici les définitions à résoudre pour ce tour. Trouve les mots correspondants.
            
            LISTE DES DÉFINITIONS :
            $wordsList
        """.trimIndent()
    }

    fun parseBatchResponse(response: String, wordCount: Int): Map<Int, Pair<String, Int>> {
        val results = mutableMapOf<Int, Pair<String, Int>>()
        // Regex un peu plus souple pour gérer d'éventuels espaces superflus
        val regex = Regex("""\[(\d+)]\s*([A-Z]+)\s*(\d+)""", RegexOption.IGNORE_CASE)

        response.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val (indexStr, word, confidenceStr) = match.destructured
                val index = indexStr.toIntOrNull()
                val confidence = confidenceStr.toIntOrNull()

                if (index != null && confidence != null && index in 1..wordCount) {
                    // On nettoie le mot ici aussi par sécurité via StringUtils
                    results[index - 1] = StringUtils.cleanAnswer(word) to confidence
                }
            }
        }
        return results
    }
}