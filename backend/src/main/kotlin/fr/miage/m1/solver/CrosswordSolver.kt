package fr.miage.m1.solver

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.WordEntry

/**
 * Service de résolution de grilles de mots croisés via LLM
 */
class CrosswordSolver(private val apiKey: String) {
    
    private val systemPrompt = """
        Expert en mots croisés français. Tu dois trouver des mots français correspondant exactement aux définitions et contraintes données.
        
        RÈGLES STRICTES :
        - Tous les mots DOIVENT être en français
        - Respecter EXACTEMENT le nombre de lettres demandé
        - Respecter tous les patterns de contraintes fournis
        - Les mots peuvent être au pluriel
        - Pour les noms propres, supprimer les accents et espaces
        
        Réponds toujours au format demandé avec le mot et ton niveau de confiance.
    """.trimIndent()
    
    /**
     * Résout une grille de mots croisés
     * 
     * @param grid La grille à résoudre (sera modifiée en place.)
     * @param maxRounds Nombre maximum de rounds de résolution
     * @param onProgress Callback appelé après chaque round avec le nombre de mots résolus
     * @return La grille avec les réponses remplies
     */
    suspend fun solve(
        grid: CrosswordGrid,
        maxRounds: Int = 10,
        onProgress: ((round: Int, solved: Int, total: Int) -> Unit)? = null
    ): CrosswordGrid {
        println("=" .repeat(60))
        println("SOLVEUR DE MOTS CROISÉS - DÉMARRAGE")
        println("=" .repeat(60))
        println("Grille: ${grid.width}x${grid.height}, ${grid.getTotalCount()} mots")
        println()
        
        var roundNumber = 0
        var previousSolvedCount = 0
        
        while (grid.getSolvedCount() < grid.getTotalCount() && roundNumber < maxRounds) {
            roundNumber++
            
            println("-".repeat(60))
            println("ROUND #$roundNumber")
            println("-".repeat(60))
            
            val unsolvedWords = grid.words.filter { !it.isSolved() }
            
            if (unsolvedWords.isEmpty()) {
                println("[INFO] Tous les mots sont résolus!")
                break
            }
            
            println("Mots non résolus: ${unsolvedWords.size}")
            
            // Priorité : mots longs d'abord, puis par nombre de contraintes
            val prioritizedWords = unsolvedWords.sortedByDescending { word ->
                when {
                    word.size >= 8 -> 1000 + word.size
                    word.size == 2 -> -100
                    else -> grid.getConstraints(word).size
                }
            }
            
            // Construire les mots avec leurs contraintes
            val wordsWithConstraints = prioritizedWords.map { word ->
                word to grid.getConstraints(word)
            }
            
            val batchPrompt = ConstraintBuilder.buildBatchPrompt(wordsWithConstraints)
            
            try {
                val agent = AIAgent(
                    promptExecutor = simpleGoogleAIExecutor(apiKey),
                    systemPrompt = systemPrompt,
                    temperature = 0.3,
                    maxIterations = 10,
                    llmModel = GoogleModels.Gemini2_5Flash
                )
                
                val response = agent.run(batchPrompt).trim()
                
                println("Réponse reçue (${response.length} caractères)")
                
                val batchResults = ConstraintBuilder.parseBatchResponse(response, prioritizedWords.size)
                
                if (batchResults.isEmpty()) {
                    println("[ERREUR] Aucune réponse valide. Fin du round.")
                    continue
                }
                
                // Phase 1: Valider les candidats
                val validCandidates = mutableListOf<WordCandidate>()
                
                batchResults.forEach { (index, result) ->
                    val word = prioritizedWords[index]
                    val (answer, confidence) = result
                    val normalizedAnswer = StringUtils.cleanAnswer(answer)
                    
                    // Vérifier la longueur
                    if (normalizedAnswer.length != word.size) {
                        println("[${word.idKey}] REJETÉ: longueur ${normalizedAnswer.length} != ${word.size}")
                        return@forEach
                    }
                    
                    // Vérifier les contraintes
                    val constraints = grid.getConstraints(word)
                    val isValid = constraints.all { (idx, expectedChar) ->
                        normalizedAnswer.getOrNull(idx)?.uppercaseChar() == expectedChar.uppercaseChar()
                    }
                    
                    if (!isValid) {
                        println("[${word.idKey}] REJETÉ: ne respecte pas les contraintes")
                        return@forEach
                    }
                    
                    validCandidates.add(WordCandidate(word, normalizedAnswer, confidence))
                    println("[${word.idKey}] VALIDE: $normalizedAnswer ($confidence%)")
                }
                
                // Phase 2 : Résolution des conflits
                val rejectedWords = resolveConflicts(validCandidates)
                
                // Phase 3 : Placer les mots gagnants
                val winners = validCandidates.filter { it.word !in rejectedWords }
                winners.forEach { candidate ->
                    grid.fillWord(candidate.word, candidate.answer)
                    candidate.word.confidence = candidate.confidence
                }
                
                println("Mots placés ce round: ${winners.size}")
                
                // Auto-compléter les mots par intersection
                grid.words.forEach { word ->
                    val wasSolved = word.isSolved()
                    grid.tryAutoFillWord(word)
                    if (!wasSolved && word.isSolved()) {
                        println("[${word.idKey}] Auto-complété: ${word.answer}")
                    }
                }
                
                // Détecter et gérer les conflits post-placement
                val conflicts = grid.detectConflicts()
                if (conflicts.isNotEmpty()) {
                    println("CONFLITS DÉTECTÉS: ${conflicts.size}")
                    conflicts.forEach { (word1, word2) ->
                        grid.removeWord(word1)
                        grid.removeWord(word2)
                        println("Supprimé: ${word1.idKey} et ${word2.idKey}")
                    }
                }
                
                onProgress?.invoke(roundNumber, grid.getSolvedCount(), grid.getTotalCount())
                
                println("\nGrille après round #$roundNumber (${grid.getSolvedCount()}/${grid.getTotalCount()}):")
                println(grid.display())
                
                if (grid.getSolvedCount() == previousSolvedCount) {
                    println("[WARNING] Aucun progrès. Arrêt.")
                    break
                }
                previousSolvedCount = grid.getSolvedCount()
                
            } catch (e: Exception) {
                println("[ERREUR] Exception: ${e.message}")
                e.printStackTrace()
                break
            }
        }
        
        println("=" .repeat(60))
        println("RÉSOLUTION TERMINÉE")
        println("Mots résolus: ${grid.getSolvedCount()}/${grid.getTotalCount()}")
        println("Rounds: $roundNumber")
        println("=" .repeat(60))
        
        return grid
    }
    
    /**
     * Résout les conflits entre candidats en gardant ceux avec la plus haute confiance
     */
    private fun resolveConflicts(candidates: List<WordCandidate>): Set<WordEntry> {
        val conflictMap = mutableMapOf<Pair<Int, Int>, MutableList<WordCandidate>>()
        
        candidates.forEach { candidate ->
            candidate.word.getCellPositions().forEachIndexed { idx, pos ->
                val char = candidate.answer[idx]
                
                candidates.filter { it != candidate }.forEach { other ->
                    val otherPositions = other.word.getCellPositions()
                    val otherIdx = otherPositions.indexOf(pos)
                    if (otherIdx >= 0) {
                        val otherChar = other.answer[otherIdx]
                        if (char != otherChar) {
                            conflictMap.getOrPut(pos) { mutableListOf() }.apply {
                                if (!contains(candidate)) add(candidate)
                                if (!contains(other)) add(other)
                            }
                        }
                    }
                }
            }
        }
        
        val rejected = mutableSetOf<WordEntry>()
        
        conflictMap.forEach { (_, conflicting) ->
            if (conflicting.size > 1) {
                val sorted = conflicting.sortedByDescending { it.confidence }
                sorted.drop(1).forEach { rejected.add(it.word) }
            }
        }
        
        return rejected
    }
    
    private data class WordCandidate(
        val word: WordEntry,
        val answer: String,
        val confidence: Int
    )
}
