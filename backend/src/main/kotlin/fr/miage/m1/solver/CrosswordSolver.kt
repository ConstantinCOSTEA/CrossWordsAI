package fr.miage.m1.solver

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.WordEntry
import kotlinx.coroutines.delay

class CrosswordSolver(private val apiKey: String) {

    // On initialise l'agent une seule fois (lazy) pour économiser des ressources.
    // Temperature basse (0.1) pour être très strict sur les contraintes.
    private val agent by lazy {
        AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            systemPrompt = ConstraintBuilder.SYSTEM_PROMPT,
            temperature = 0.1,
            llmModel = GoogleModels.Gemini2_5Flash
        )
    }

    suspend fun solve(
        grid: CrosswordGrid,
        maxRounds: Int = 10,
        onRoundComplete: (suspend (round: Int, solvedWords: List<WordEntry>) -> Unit)? = null
    ): CrosswordGrid {
        logHeader("DÉMARRAGE SOLVEUR", "Grille: ${grid.width}x${grid.height}, ${grid.getTotalCount()} mots")

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

            // 2. Préparer le prompt
            val wordsWithConstraints = unsolvedWords.map { word ->
                word to grid.getConstraints(word)
            }
            val batchPrompt = ConstraintBuilder.buildBatchPrompt(wordsWithConstraints)

            try {
                // 3. Appel IA
                val response = agent.run(batchPrompt).trim()

                // 4. Parsing de la réponse
                val batchResults = ConstraintBuilder.parseBatchResponse(response, unsolvedWords.size)

                if (batchResults.isEmpty()) {
                    println("[INFO] Aucune réponse valide reçue ce round.")
                    consecutiveEmptyRounds++

                    // SÉCURITÉ : Si l'IA échoue 3 fois de suite, on arrête pour ne pas insister inutilement
                    if (consecutiveEmptyRounds >= 3) {
                        println("[STOP] L'IA ne trouve plus rien depuis 3 tours. Arrêt de la résolution.")
                        break
                    }

                    // Petit délai avant de retenter pour laisser respirer l'API
                    delay(2000)
                    continue
                } else {
                    // On a trouvé des mots, on reset le compteur d'échecs
                    consecutiveEmptyRounds = 0
                }

                // 5. Traitement et validation des résultats
                processRoundResults(grid, unsolvedWords, batchResults)

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
                // Gestion spécifique de l'erreur 429 (Quota)
                if (e.message?.contains("429") == true) {
                    println("[CRITIQUE] Quota Google Gemini dépassé (Erreur 429). Arrêt d'urgence.")
                    break
                }
                println("[ERREUR] Problème lors du round: ${e.message}")
                e.printStackTrace()
                break // On sort de la boucle en cas d'erreur technique grave
            }

            // --- CRUCIAL : TEMPORISATION ---
            // On force une pause de 4 secondes entre chaque round.
            // Cela limite les appels à ~15 par minute, ce qui est safe pour le Free Tier.
            println("Pause API (4s)...")
            delay(4000)
        }

        logHeader("FIN DE RÉSOLUTION", "Total résolu: ${grid.getSolvedCount()}/${grid.getTotalCount()}")
        return grid
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
     * Traite les réponses de l'IA : Validation -> Résolution de conflits -> Remplissage
     */
    private fun processRoundResults(
        grid: CrosswordGrid,
        originalList: List<WordEntry>,
        results: Map<Int, Pair<String, Int>>
    ) {
        val candidates = mutableListOf<WordCandidate>()

        // A. Validation syntaxique (longueur + respect des lettres déjà là)
        results.forEach { (index, res) ->
            val (answer, confidence) = res
            val wordEntry = originalList.getOrNull(index) ?: return@forEach

            if (isValidCandidate(grid, wordEntry, answer)) {
                candidates.add(WordCandidate(wordEntry, answer, confidence))
            } else {
                // Log discret pour debug
                // println("[REJET] ${wordEntry.clue.take(15)}... -> $answer (Ne correspond pas à la grille)")
            }
        }

        // B. Résolution des conflits (si deux mots veulent mettre une lettre différente au même endroit)
        val rejectedWords = resolveConflicts(candidates)
        val winners = candidates.filter { it.word !in rejectedWords }

        // C. Application dans la grille
        winners.forEach { cand ->
            grid.fillWord(cand.word, cand.answer)
            cand.word.confidence = cand.confidence
            println("[VALIDE] [${cand.word.number}-${cand.word.direction}] ${cand.answer} (${cand.confidence}%)")
        }

        // D. Auto-complétion (Si un mot vertical est entièrement rempli par les horizontaux, on le marque résolu)
        grid.words.forEach { grid.tryAutoFillWord(it) }
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
        val conflictMap = mutableMapOf<Pair<Int, Int>, MutableList<WordCandidate>>()
        val rejected = mutableSetOf<WordEntry>()

        // 1. Mapper chaque lettre proposée à sa coordonnée (x,y)
        candidates.forEach { candidate ->
            candidate.word.coordinates.forEachIndexed { idx, pos ->
                val char = candidate.answer[idx]

                // On compare avec tous les autres candidats
                candidates.filter { it != candidate }.forEach { other ->
                    val otherIdx = other.word.coordinates.indexOf(pos)
                    // Si l'autre candidat passe par la même case
                    if (otherIdx >= 0) {
                        val otherChar = other.answer[otherIdx]
                        // Et qu'ils ne sont pas d'accord sur la lettre
                        if (otherChar != char) {
                            conflictMap.getOrPut(pos) { mutableListOf() }.apply {
                                add(candidate)
                                if (!contains(other)) add(other)
                            }
                        }
                    }
                }
            }
        }

        // 2. Résoudre les conflits par vote de confiance
        conflictMap.values.forEach { conflictingCandidates ->
            if (conflictingCandidates.size > 1) {
                // On trie par confiance décroissante
                val sorted = conflictingCandidates.sortedByDescending { it.confidence }
                val winner = sorted.first()

                // Tous les autres sont rejetés
                sorted.drop(1).forEach { loser ->
                    rejected.add(loser.word)
                    println("[CONFLIT] ${loser.answer} rejeté au profit de ${winner.answer} (Conf: ${winner.confidence} vs ${loser.confidence})")
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
        // Décommenter pour afficher la grille ASCII dans la console (utile pour debug)
        println(grid.display())
    }

    // Classe interne simple pour stocker les propositions avant validation
    private data class WordCandidate(val word: WordEntry, val answer: String, val confidence: Int)
}