package fr.miage.m1

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import fr.miage.m1.ai.ConstraintBuilder
import fr.miage.m1.model.Word
import fr.miage.m1.parser.CrosswordJsonParser
import fr.miage.m1.utils.StringUtils
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    println("=".repeat(60))
    println("SOLVEUR DE MOTS CROISES AVEC IA (STRATEGIE BATCH)")
    println("=".repeat(60))
    println()

    val apiKey = System.getenv("GOOGLE_API_KEY")
    val systemPrompt = """
        Expert en mots croises francais. Tu dois trouver des mots francais correspondant exactement aux definitions et contraintes donnees.
        
        REGLES STRICTES :
        - Tous les mots DOIVENT etre en francais
        - Respecter EXACTEMENT le nombre de lettres demande
        - Respecter tous les patterns de contraintes fournis
        - Les mots peuvent etre au pluriel
        - Pour les noms propres, supprimer les accents et espaces
        
        Reponds toujours au format demande avec le mot et ton niveau de confiance.
    """.trimIndent()

    // Charger le JSON des mots croisés
    val jsonFile = File("src/main/resources/crossword_questions_test.json")
    if (!jsonFile.exists()) {
        error("Le fichier crossword_questions_test.json n'existe pas.")
    }

    val jsonContent = jsonFile.readText()

    // Parser la grille
    println("Chargement de la grille de mots croises...")
    val grid = CrosswordJsonParser.parse(jsonContent)
    println("Grille chargee: ${grid.width}x${grid.height}, ${grid.getTotalCount()} mots")
    println()

    // Afficher la grille initiale
    println("Grille initiale:")
    println(grid.displayGrid())
    println()

    println("=".repeat(60))
    println("DEMARRAGE DE LA RESOLUTION PAR BATCH")
    println("=".repeat(60))
    println()

    var roundNumber = 0
    val maxRounds = 10
    var previousSolvedCount = 0

    // Boucle de résolution itérative
    while (grid.getSolvedCount() < grid.getTotalCount() && roundNumber < maxRounds) {
        roundNumber++

        println("-".repeat(60))
        println("ROUND #$roundNumber")
        println("-".repeat(60))
        println()

        // Obtenir les mots non résolus
        val unsolvedWords = grid.words.filter { !it.isSolved() }

        if (unsolvedWords.isEmpty()) {
            println("[INFO] Tous les mots sont resolus!")
            break
        }

        println("Mots non resolus: ${unsolvedWords.size}")

        // Favoriser les mots longs (8+ lettres) d'abord
        val prioritizedWords = unsolvedWords.sortedByDescending { word ->
            when {
                word.length() >= 8 -> 1000 + word.length() // Mots très longs en priorité
                word.length() == 2 -> -100 // Mots de 2 lettres en dernier (piégeux)
                else -> word.getConstraints().size // Sinon, par nombre de contraintes
            }
        }

        println()
        println("Interrogation de l'IA avec BATCH de ${prioritizedWords.size} definitions...")

        // Construire et envoyer le prompt batch
        val batchPrompt = ConstraintBuilder.buildBatchPrompt(prioritizedWords)

        try {
            val agent = AIAgent(
                promptExecutor = simpleGoogleAIExecutor(apiKey),
                systemPrompt = systemPrompt,
                temperature = 0.3,
                maxIterations = 10,
                llmModel = GoogleModels.Gemini2_5Flash
            )

            val response = agent.run(batchPrompt).trim()

            println()
            println("Reponse brute:")
            println(response)
            println()

            // Parser les réponses
            val batchResults = ConstraintBuilder.parseBatchResponse(response, prioritizedWords.size)

            if (batchResults.isEmpty()) {
                println("[ERREUR] Aucune reponse valide parsee. Fin du round.")
                continue
            }

            println("Reponses parsees: ${batchResults.size}")
            println()

            // PHASE 1: Valider tous les candidats SANS les placer
            data class WordCandidate(
                val word: Word,
                val answer: String,
                val confidence: Int
            )

            val validCandidates = mutableListOf<WordCandidate>()

            println("=== PHASE 1: VALIDATION DES CANDIDATS ===")
            println()

            batchResults.forEach { (index, result) ->
                val word = prioritizedWords[index]
                val (answer, confidence) = result
                val normalizedAnswer = StringUtils.removeAccents(answer)

                println("[${word.number}-${word.order}] \"${word.definition}\"")
                println("  -> Proposition: $answer${if (answer != normalizedAnswer) " -> $normalizedAnswer" else ""}")
                println("  -> Confiance: $confidence%")

                // Vérifier la longueur
                if (normalizedAnswer.length != word.length()) {
                    println("  -> REJETE: Longueur incorrecte (attendu ${word.length()}, recu ${normalizedAnswer.length})")
                    println()
                    return@forEach
                }

                // Vérifier les contraintes ACTUELLES (celles déjà dans la grille)
                val constraints = word.getConstraints()
                val isValid = constraints.all { (idx, expectedChar) ->
                    normalizedAnswer.getOrNull(idx)?.uppercaseChar() == expectedChar.uppercaseChar()
                }

                if (!isValid) {
                    println("  -> REJETE: Ne respecte pas les contraintes actuelles")
                    constraints.forEach { (idx, expectedChar) ->
                        val actualChar = normalizedAnswer.getOrNull(idx)
                        if (actualChar?.uppercaseChar() != expectedChar.uppercaseChar()) {
                            println("     Position ${idx + 1}: attendu '$expectedChar', recu '$actualChar'")
                        }
                    }
                    println()
                    return@forEach
                }

                // Candidat valide!
                validCandidates.add(WordCandidate(word, normalizedAnswer, confidence))
                println("  -> CANDIDAT VALIDE (confiance: $confidence%)")
                println()
            }

            println("Candidats valides: ${validCandidates.size}")
            println()

            // PHASE 2: Résolution des conflits par score de confiance
            println("=== PHASE 2: RESOLUTION DES CONFLITS ===")
            println()

            // Détecter les conflits entre candidats
            val conflictMap = mutableMapOf<Pair<Int, Int>, MutableList<WordCandidate>>()

            validCandidates.forEach { candidate ->
                candidate.word.cells.forEachIndexed { idx, cell ->
                    val position = Pair(cell.x, cell.y)
                    val char = candidate.answer[idx]

                    // Chercher les autres candidats qui utilisent cette position
                    validCandidates.forEach { otherCandidate ->
                        if (otherCandidate != candidate) {
                            val otherIdx = otherCandidate.word.cells.indexOf(cell)
                            if (otherIdx >= 0) {
                                val otherChar = otherCandidate.answer[otherIdx]
                                if (char != otherChar) {
                                    // CONFLIT DETECTE!
                                    conflictMap.getOrPut(position) { mutableListOf() }.apply {
                                        if (!contains(candidate)) add(candidate)
                                        if (!contains(otherCandidate)) add(otherCandidate)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Résoudre les conflits en gardant le mot avec la plus haute confiance
            val rejectedWords = mutableSetOf<Word>()

            conflictMap.forEach { (position, candidates) ->
                if (candidates.size > 1) {
                    // Trier par confiance décroissante
                    val sorted = candidates.sortedByDescending { it.confidence }
                    val winner = sorted.first()
                    val losers = sorted.drop(1)

                    println("CONFLIT en position ($position):")
                    sorted.forEach { candidate ->
                        val cellIdx = candidate.word.cells.indexOfFirst { it.x == position.first && it.y == position.second }
                        val char = candidate.answer[cellIdx]
                        val status = if (candidate == winner) "GAGNE" else "PERDU"
                        println("  - [${candidate.word.number}-${candidate.word.order}] '${candidate.answer}' ($char) - ${candidate.confidence}% -> $status")
                    }

                    // Rejeter les perdants
                    losers.forEach { rejectedWords.add(it.word) }
                    println()
                }
            }

            if (conflictMap.isEmpty()) {
                println("Aucun conflit entre candidats!")
                println()
            } else {
                println("Mots rejetes par conflit: ${rejectedWords.size}")
                println()
            }

            // PHASE 3: Placer les mots gagnants
            println("=== PHASE 3: PLACEMENT DES MOTS GAGNANTS ===")
            println()

            val winners = validCandidates.filter { it.word !in rejectedWords }
            val roundPlacements = mutableListOf<Pair<Word, String>>()

            winners.forEach { candidate ->
                grid.fillWord(candidate.word, candidate.answer)
                roundPlacements.add(candidate.word to candidate.answer)
                println("  -> [${candidate.word.number}-${candidate.word.order}] '${candidate.answer}' place (confiance: ${candidate.confidence}%)")
            }

            println()
            println("Mots places ce round: ${roundPlacements.size}")
            println()

            // Auto-compléter les mots entièrement remplis par intersection
            println("Verification des mots auto-completes par intersection...")
            var autoFilledCount = 0
            grid.words.forEach { word ->
                val wasSolved = word.isSolved()
                word.tryAutoFill()
                if (!wasSolved && word.isSolved()) {
                    autoFilledCount++
                    println("  - [${word.number}-${word.order}] Auto-complete: ${word.answer}")
                }
            }
            if (autoFilledCount > 0) {
                println("Mots auto-completes: $autoFilledCount")
            } else {
                println("Aucun mot auto-complete.")
            }
            println()

            // Détecter les conflits post-placement (entre rounds différents)
            println("Detection des conflits post-placement...")
            val conflicts = grid.detectConflicts()

            if (conflicts.isNotEmpty()) {
                println("CONFLITS DETECTES: ${conflicts.size}")
                println()

                // Enlever tous les mots en conflit
                val wordsToRemove = mutableSetOf<Word>()
                conflicts.forEach { (word1, word2) ->
                    wordsToRemove.add(word1)
                    wordsToRemove.add(word2)
                    println("  - Conflit entre [${word1.number}-${word1.order}] et [${word2.number}-${word2.order}]")
                }

                println()
                println("Suppression de ${wordsToRemove.size} mot(s) en conflit...")
                wordsToRemove.forEach { word ->
                    println("  - Supprime: [${word.number}-${word.order}] ${word.answer}")
                    grid.removeWord(word)
                }
                println()
            } else {
                println("Aucun conflit post-placement!")
                println()
            }

            // Afficher la grille après ce round
            println("Grille apres round #$roundNumber (${grid.getSolvedCount()}/${grid.getTotalCount()} mots):")
            println(grid.displayGrid())
            println()

            // Vérifier si on a fait des progrès
            if (grid.getSolvedCount() == previousSolvedCount) {
                println("[WARNING] Aucun progres ce round. Arret.")
                break
            }
            previousSolvedCount = grid.getSolvedCount()

        } catch (e: Exception) {
            println("[ERREUR] Exception lors de l'interrogation: ${e.message}")
            e.printStackTrace()
            break
        }
    }

    // Afficher la grille finale
    println("=".repeat(60))
    println("RESOLUTION TERMINEE")
    println("=".repeat(60))
    println()
    println("Grille finale:")
    println(grid.displayGrid())
    println()
    println("Statistiques:")
    println("- Mots resolus: ${grid.getSolvedCount()}/${grid.getTotalCount()}")
    println("- Rounds effectues: $roundNumber")
    println("- Taux de reussite: ${(grid.getSolvedCount() * 100) / grid.getTotalCount()}%")
    println()
}