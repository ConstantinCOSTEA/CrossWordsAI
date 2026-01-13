package fr.miage.m1.solver

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.WordEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class CrosswordSolver(private val apiKey: String) {

    /**
     * Résout la grille de mots croisés en utilisant plusieurs agents IA en parallèle.
     * @param grid La grille à résoudre
     * @param maxRounds Nombre maximum de tours de résolution
     * @param nbAgents Nombre d'agents IA à utiliser en parallèle (divise le travail)
     * @param onRoundComplete Callback appelé à chaque tour avec les mots résolus
     */
    suspend fun solve(
        grid: CrosswordGrid,
        maxRounds: Int = 10,
        nbAgents: Int = 2,
        onRoundComplete: (suspend (round: Int, solvedWords: List<WordEntry>) -> Unit)? = null
    ): CrosswordGrid {
        logHeader("DÉMARRAGE SOLVEUR", "Grille: ${grid.width}x${grid.height}, ${grid.getTotalCount()} mots, $nbAgents agents en parallèle")

        var roundNumber = 0
        var previousSolvedCount = 0
        var consecutiveEmptyRounds = 0 // Compteur pour détecter si l'IA tourne en rond

        // Boucle principale
        while (grid.getSolvedCount() < grid.getTotalCount() && roundNumber < maxRounds) {
            roundNumber++
            println("\n--- ROUND #$roundNumber (Résolus: ${grid.getSolvedCount()}/${grid.getTotalCount()}) ---")

            // 1. Sélectionner les mots à chercher (priorité aux mots longs ou à trous)
            val unsolvedWords = getPrioritizedUnsolvedWords(grid)

            if (unsolvedWords.isEmpty()) {
                println("Tous les mots sont résolus !")
                break
            }

            try {
                // 2. Diviser les mots en N groupes pour les agents parallèles
                val wordGroups = splitIntoGroups(unsolvedWords, nbAgents)
                println("[PARALLÉLISATION] ${unsolvedWords.size} mots divisés en ${wordGroups.size} groupes")

                // 3. Appels IA en parallèle avec coroutines
                val allResults = coroutineScope {
                    wordGroups.mapIndexed { groupIndex, group ->
                        async {
                            callAgentForGroup(groupIndex + 1, group, grid)
                        }
                    }.awaitAll()
                }

                // 4. Fusionner tous les résultats (avec ajustement des indices)
                val mergedResults = mergeResults(allResults, wordGroups)

                if (mergedResults.isEmpty()) {
                    println("[INFO] Aucune réponse valide reçue ce round.")
                    consecutiveEmptyRounds++

                    // SÉCURITÉ : Si l'IA échoue 3 fois de suite, on arrête pour ne pas insister inutilement
                    if (consecutiveEmptyRounds >= 3) {
                        println("[STOP] L'IA ne trouve plus rien depuis 3 tours. Arrêt de la résolution.")
                        break
                    }

                    continue
                } else {
                    // On a trouvé des mots, on remet à 0 le compteur d'échecs
                    consecutiveEmptyRounds = 0
                }

                // 5. Traitement et validation des résultats
                processRoundResults(grid, unsolvedWords, mergedResults)

                // 6. Notification (Callback pour le SSE)
                val solvedNow = grid.words.filter { it.isSolved() }
                onRoundComplete?.invoke(roundNumber, solvedNow)

                logProgress(grid)

                // 7. Vérification de la stagnation (Si on n'avance plus, on arrête)
                if (grid.getSolvedCount() == previousSolvedCount) {
                    println("[STOP] Stagnation détectée (pas de nouveaux mots validés).")
                    break
                }
                previousSolvedCount = grid.getSolvedCount()

            } catch (e: Exception) {
                println("[ERREUR] Problème lors du round: ${e.message}")
                break
            }
        }

        logHeader("FIN DE RÉSOLUTION", "Total résolu: ${grid.getSolvedCount()}/${grid.getTotalCount()}")
        return grid
    }

    /**
     * Divise une liste en N groupes équilibrés.
     */
    private fun <T> splitIntoGroups(items: List<T>, n: Int): List<List<T>> {
        if (items.isEmpty() || n <= 0) return listOf(items)
        val effectiveN = minOf(n, items.size) // Pas plus de groupes que d'éléments
        val chunkSize = (items.size + effectiveN - 1) / effectiveN
        return items.chunked(chunkSize)
    }

    /**
     * Appelle un agent IA pour un groupe de mots spécifique.
     * Utilise le StructuredPromptBuilder avec few-shot learning pour améliorer les résultats.
     * Inclut un timeout de 60 secondes pour éviter les blocages.
     */
    private suspend fun callAgentForGroup(
        agentId: Int,
        words: List<WordEntry>,
        grid: CrosswordGrid
    ): Map<Int, Triple<String, Int, Boolean>> {
        if (words.isEmpty()) return emptyMap()

        println("[AGENT #$agentId] Traite ${words.size} définitions...")

        val wordsWithConstraints = words.map { word ->
            word to grid.getConstraints(word)
        }
        
        // Utilise le nouveau StructuredPromptBuilder avec few-shot learning
        val userPrompt = StructuredPromptBuilder.buildUserPrompt(wordsWithConstraints)

        val agent = AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            systemPrompt = StructuredPromptBuilder.SYSTEM_PROMPT_WITH_EXAMPLES,
            temperature = 0.1,
            llmModel = GoogleModels.Gemini2_5Flash
        )

        // Timeout de 35 secondes par agent pour éviter les blocages
        val response = withTimeoutOrNull(35_000L) {
            agent.run(userPrompt).trim()
        }
        
        if (response == null) {
            println("[AGENT #$agentId] ⚠️ TIMEOUT après 35s - Aucune réponse")
            return emptyMap()
        }
        
        val results = ConstraintBuilder.parseBatchResponse(response, words.size)

        val altCount = results.count { it.value.third }
        println("[AGENT #$agentId] A trouvé ${results.size} réponses (dont $altCount ALT)")
        return results
    }

    /**
     * Fusionne les résultats de tous les agents en réajustant les indices
     * pour correspondre à la liste originale unsolvedWords.
     */
    private fun mergeResults(
        allResults: List<Map<Int, Triple<String, Int, Boolean>>>,
        wordGroups: List<List<WordEntry>>
    ): Map<Int, Triple<String, Int, Boolean>> {
        val merged = mutableMapOf<Int, Triple<String, Int, Boolean>>()
        var globalOffset = 0

        allResults.forEachIndexed { groupIndex, groupResults ->
            groupResults.forEach { (localIndex, answer) ->
                val globalIndex = globalOffset + localIndex
                merged[globalIndex] = answer
            }
            globalOffset += wordGroups.getOrNull(groupIndex)?.size ?: 0
        }

        return merged
    }

    /**
     * Trie les mots non résolus par priorité :
     * 1. Mots longs (plus faciles à trouver pour l'IA sans contexte)
     * 2. Mots ayant déjà des lettres placées (contraintes)
     */
    private fun getPrioritizedUnsolvedWords(grid: CrosswordGrid): List<WordEntry> {
        return grid.words.filter { !it.isSolved() }
            .sortedByDescending { word ->
                val constraintsCount = grid.getConstraints(word).size
                when {
                    word.size >= 8 -> 100 + word.size // Bonus mots longs
                    constraintsCount > 0 -> 50 + constraintsCount // Bonus mots avec indices
                    else -> 0
                }
            }
    }

    /**
     * Traite les réponses de l'IA : Validation → Résolution de conflits → Remplissage
     */
    private fun processRoundResults(
        grid: CrosswordGrid,
        originalList: List<WordEntry>,
        results: Map<Int, Triple<String, Int, Boolean>>
    ) {
        val candidates = mutableListOf<WordCandidate>()

        // A. Validation syntaxique (longueur + respect des lettres déjà là)
        // Les réponses ALT ignorent les contraintes (utile si les contraintes sont fausses)
        results.forEach { (index, res) ->
            val (answer, confidence, isAlternative) = res
            val wordEntry = originalList.getOrNull(index) ?: return@forEach

            if (isAlternative) {
                // Réponse ALT : l'IA a ignoré les contraintes, on valide juste la longueur
                if (answer.length == wordEntry.size) {
                    candidates.add(WordCandidate(wordEntry, answer, confidence, isAlternative = true))
                    println("[ALT] ${wordEntry.clue.take(20)}... -> $answer (contraintes ignorées)")
                }
            } else if (isValidCandidate(grid, wordEntry, answer)) {
                candidates.add(WordCandidate(wordEntry, answer, confidence, isAlternative = false))
            }
        }

        // B. Résolution des conflits (si deux mots veulent mettre une lettre différente au même endroit)
        val rejectedWords = resolveConflicts(candidates)
        val winners = candidates.filter { it.word !in rejectedWords }

        // C. Application dans la grille avec gestion des conflits avec les mots existants
        winners.forEach { cand ->
            val conflictingWord = findConflictWithExistingWord(grid, cand)
            
            if (conflictingWord != null) {
                val existingConfidence = conflictingWord.confidence
                val newConfidence = cand.confidence
                val diff = kotlin.math.abs(existingConfidence - newConfidence)
                
                if (diff < 5) {
                    // Différence < 5% → on enlève les deux (trop incertain)
                    println("[CONFLIT GRILLE] ${cand.answer} vs ${conflictingWord.answer}: Diff $diff% < 5% → Les deux retirés")
                    grid.removeWord(conflictingWord)
                    // On ne place pas le nouveau mot non plus
                } else if (newConfidence > existingConfidence) {
                    // Le nouveau est plus confiant → on remplace
                    println("[CONFLIT GRILLE] ${cand.answer} (${newConfidence}%) remplace ${conflictingWord.answer} (${existingConfidence}%)")
                    grid.removeWord(conflictingWord)
                    grid.fillWord(cand.word, cand.answer)
                    cand.word.confidence = cand.confidence
                } else {
                    // L'ancien est plus confiant → on garde l'ancien
                    println("[CONFLIT GRILLE] ${cand.answer} (${newConfidence}%) rejeté, ${conflictingWord.answer} (${existingConfidence}%) conservé")
                }
            } else {
                // Pas de conflit, placement normal
                grid.fillWord(cand.word, cand.answer)
                cand.word.confidence = cand.confidence
                println("[VALIDE] [${cand.word.number}-${cand.word.direction}] ${cand.answer} (${cand.confidence}%)")
            }
        }

        // D. Auto-complétion (Si un mot vertical est entièrement rempli par les horizontaux, on le marque résolu)
        grid.words.forEach { grid.tryAutoFillWord(it) }
    }
    
    /**
     * Vérifie si un candidat entre en conflit avec un mot déjà placé dans la grille.
     * Un conflit = même position, mais lettre différente.
     */
    private fun findConflictWithExistingWord(grid: CrosswordGrid, candidate: WordCandidate): WordEntry? {
        val solvedWords = grid.words.filter { it.isSolved() && it != candidate.word }
        
        for (existingWord in solvedWords) {
            // Trouver les positions communes
            val candidatePositions = candidate.word.coordinates
            val existingPositions = existingWord.coordinates
            
            for ((candIdx, pos) in candidatePositions.withIndex()) {
                val existingIdx = existingPositions.indexOf(pos)
                if (existingIdx >= 0) {
                    // Position commune trouvée
                    val candidateChar = candidate.answer[candIdx]
                    val existingChar = existingWord.answer?.getOrNull(existingIdx)
                    
                    if (existingChar != null && candidateChar != existingChar) {
                        // Conflit détecté !
                        return existingWord
                    }
                }
            }
        }
        return null
    }

    private fun isValidCandidate(grid: CrosswordGrid, word: WordEntry, answer: String): Boolean {
        if (answer.length != word.size) return false

        // Vérifie si le nouveau mot respecte les lettres DÉJÀ présentes dans la grille
        val constraints = grid.getConstraints(word)
        return constraints.all { (idx, char) ->
            answer[idx] == char
        }
    }

    /**
     * Détecte si deux candidats proposés par l'IA entrent en collision
     * (ex: H1 veut mettre 'A' à (1,1) et V1 veut mettre 'B' à (1,1))
     * On garde celui qui a la plus haute confiance.
     */
    private fun resolveConflicts(candidates: List<WordCandidate>): Set<WordEntry> {
        val rejected = mutableSetOf<WordEntry>()
        val alreadyProcessed = mutableSetOf<Pair<WordEntry, WordEntry>>()

        // Comparer chaque paire de candidats une seule fois
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val candidateA = candidates[i]
                val candidateB = candidates[j]
                
                // Éviter de traiter la même paire deux fois
                val pairKey = if (candidateA.word.hashCode() < candidateB.word.hashCode()) {
                    candidateA.word to candidateB.word
                } else {
                    candidateB.word to candidateA.word
                }
                if (pairKey in alreadyProcessed) continue
                alreadyProcessed.add(pairKey)
                
                // Vérifier s'il y a un conflit de lettres entre ces deux candidats
                val hasConflict = candidateA.word.coordinates.withIndex().any { (idxA, posA) ->
                    val idxB = candidateB.word.coordinates.indexOf(posA)
                    if (idxB >= 0) {
                        // Même position - vérifier si les lettres diffèrent
                        candidateA.answer[idxA] != candidateB.answer[idxB]
                    } else {
                        false
                    }
                }
                
                if (hasConflict) {
                    // Conflit détecté - rejeter celui avec la plus faible confiance
                    if (candidateA.confidence >= candidateB.confidence) {
                        rejected.add(candidateB.word)
                        println("[CONFLIT] ${candidateB.answer} rejeté au profit de ${candidateA.answer} (Conf: ${candidateA.confidence}% vs ${candidateB.confidence}%)")
                    } else {
                        rejected.add(candidateA.word)
                        println("[CONFLIT] ${candidateA.answer} rejeté au profit de ${candidateB.answer} (Conf: ${candidateB.confidence}% vs ${candidateA.confidence}%)")
                    }
                }
            }
        }

        return rejected
    }

    private fun logHeader(title: String, subtitle: String) {
        println("=".repeat(60))
        println(title)
        println(subtitle)
        println("=".repeat(60))
    }

    private fun logProgress(grid: CrosswordGrid) {
        println(grid.display())
    }

    // Classe interne simple pour stocker les propositions avant validation
    // isAlternative = true si l'IA a ignoré les contraintes (flag ALT)
    private data class WordCandidate(
        val word: WordEntry, 
        val answer: String, 
        val confidence: Int,
        val isAlternative: Boolean = false
    )
}