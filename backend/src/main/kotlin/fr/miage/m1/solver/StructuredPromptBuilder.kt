package fr.miage.m1.solver

import fr.miage.m1.model.WordEntry

/**
 * Construit des prompts avec few-shot learning pour améliorer
 * la qualité des réponses de l'IA sur les mots croisés.
 * 
 * Les exemples sont intégrés directement dans le system prompt pour montrer
 * à l'IA que les noms propres, villes, abréviations sont des réponses valides.
 */
object StructuredPromptBuilder {

    /**
     * System prompt enrichi avec des exemples few-shot.
     * Les exemples montrent que tout type de réponse est acceptable (villes, abréviations, noms propres).
     */
    val SYSTEM_PROMPT_WITH_EXAMPLES = """
        Tu es un expert en mots croisés français. Ta mission est de trouver des mots qui correspondent aux définitions.
        RÉPONDS LE PLUS RAPIDEMENT POSSIBLE. Ne réfléchis pas trop longtemps.
        
        RÈGLES IMPÉRATIVES :
        1. Langue : FRANÇAIS uniquement
        2. Longueur : Respecte STRICTEMENT le nombre de lettres attendu
        3. Format : MAJUSCULES, SANS accents, SANS espaces (ex: "SAO PAULO" → "SAOPAULO")
        4. Contraintes : Si un pattern est fourni (ex: _ A _ E), tu DOIS le respecter
        5. TYPES DE RÉPONSES ACCEPTÉS : Mots du dictionnaire, noms propres, villes, prénoms, abréviations, tout est valide !
        6. DÉBLOCAGE : Si aucun mot ne correspond avec les contraintes, propose une réponse SANS respecter le pattern et ajoute "ALT" après la confiance
        7. AUCUNE IDÉE : Si tu n'as vraiment aucune idée, réponds "?" pour passer ce mot
        
        FORMAT DE SORTIE (une ligne par réponse) :
        [ID] MOT CONFIANCE
        ou si tu ignores les contraintes :
        [ID] MOT CONFIANCE ALT
        ou si tu n'as aucune idée :
        [ID] ?
        
        ===== EXEMPLES DE RÉSOLUTION =====
        
        Exemple 1 - Nom propre (ville) :
        Entrée: [1] (5 lettres) : "Capitale de la France"
        Sortie: [1] PARIS 100
        
        Exemple 2 - Mot court :
        Entrée: [2] (2 lettres) : "Note de musique"
        Sortie: [2] DO 95
        
        Exemple 3 - Abréviation :
        Entrée: [3] (3 lettres) : "Ville du Brésil (abrév.)"
        Sortie: [3] RIO 100
        
        Exemple 4 - Pronom :
        Entrée: [4] (2 lettres) : "Pronom personnel"
        Sortie: [4] IL 100
        
        Exemple 5 - Avec contraintes :
        Entrée: [5] (6 lettres) : "Fruit rouge" | Pattern imposé: _ R _ I S _
        Sortie: [5] FRAISE 98
        
        Exemple 6 - Nom propre (personne) :
        Entrée: [6] (4 lettres) : "Écrivain français des Misérables"
        Sortie: [6] HUGO 100
        
        Exemple 7 - Réponse ALT (contraintes ignorées car impossibles) :
        Entrée: [7] (5 lettres) : "Animal domestique" | Pattern imposé: C _ I E _
        Sortie: [7] CHIEN 95 ALT
        
        Exemple 8 - Aucune idée (skip) :
        Entrée: [8] (7 lettres) : "Définition trop obscure impossible à deviner"
        Sortie: [8] ?
        
        ===== FIN DES EXEMPLES =====
        
        IMPORTANT :
        - Réponds le plus vite possible
        - Si tu bloques sur un mot à cause des contraintes, utilise ALT
        - Si tu n'as vraiment aucune idée, utilise ? pour passer
        - Ne perds pas de temps sur les mots difficiles, passe avec ? et continue
    """.trimIndent()

    /**
     * Construit le prompt utilisateur avec les définitions à résoudre.
     */
    fun buildUserPrompt(words: List<Pair<WordEntry, Map<Int, Char>>>): String {
        val wordsList = words.mapIndexed { index, (word, constraints) ->
            val constraintInfo = if (constraints.isNotEmpty()) {
                " | Pattern imposé: ${buildWordPattern(word.size, constraints)}"
            } else {
                ""
            }
            "[${index + 1}] (${word.size} lettres) : \"${word.clue}\"$constraintInfo"
        }.joinToString("\n")

        return """
            Voici les définitions à résoudre pour ce tour. Trouve les mots correspondants.
            
            LISTE DES DÉFINITIONS :
            $wordsList
        """.trimIndent()
    }

    /**
     * Construit le pattern visuel (ex : _ A _ E) à partir des contraintes.
     */
    private fun buildWordPattern(length: Int, constraints: Map<Int, Char>): String {
        return (0 until length).joinToString(" ") { index ->
            constraints[index]?.toString() ?: "_"
        }
    }
}
