package fr.miage.m1.solver

object ConstraintBuilder {

    /**
     * Parse la réponse de l'IA.
     * Retourne un Triple : (mot, confiance, isAlternative)
     * isAlternative = true si l'IA a ignoré les contraintes (flag ALT)
     */
    fun parseBatchResponse(response: String, wordCount: Int): Map<Int, Triple<String, Int, Boolean>> {
        val results = mutableMapOf<Int, Triple<String, Int, Boolean>>()
        // Regex mise à jour pour capturer le flag ALT optionnel
        val regex = Regex("""\[(\d+)]\s*([A-Z]+)\s*(\d+)\s*(ALT)?""", RegexOption.IGNORE_CASE)

        response.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val indexStr = match.groupValues[1]
                val word = match.groupValues[2]
                val confidenceStr = match.groupValues[3]
                val isAlt = match.groupValues.getOrNull(4)?.isNotEmpty() == true

                val index = indexStr.toIntOrNull()
                val confidence = confidenceStr.toIntOrNull()

                if (index != null && confidence != null && index in 1..wordCount) {
                    results[index - 1] = Triple(StringUtils.cleanAnswer(word), confidence, isAlt)
                }
            }
        }
        return results
    }
}