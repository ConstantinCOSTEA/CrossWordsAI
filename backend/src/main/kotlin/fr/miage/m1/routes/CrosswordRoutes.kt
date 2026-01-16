package fr.miage.m1.routes

import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.ocr.GridAnalyzer
import fr.miage.m1.schemas.*
import fr.miage.m1.solver.CrosswordSolver
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

fun Route.crosswordRoutes() {
    val gridAnalyzer = GridAnalyzer()

    /**
     * ÉTAPE 1 : Analyse de l'image
     * Reçoit une image (Multipart) → Renvoie la structure JSON de la grille
     */
    post("/analyze") {
        var imageFile: File? = null
        try {
            println("📥 [/analyze] Requête reçue - Analyse de grille en cours...")
            val startTime = System.currentTimeMillis()
            
            // 1. Récupération de l'image
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") {
                    val fileName = "grid_${UUID.randomUUID()}.png"
                    imageFile = File(System.getProperty("java.io.tmpdir"), fileName)
                    part.provider().toInputStream().use { input ->
                        imageFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                part.dispose()
            }

            if (imageFile == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Aucune image reçue"))
                return@post
            }

            // ==== DEBUG: Sauvegarde des images pour diagnostic ====
            val debugDir = File("debug_images")
            if (!debugDir.exists()) debugDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            val originalDebugFile = File(debugDir, "original_$timestamp.png")
            val preprocessedDebugFile = File(debugDir, "preprocessed_$timestamp.png")
            
            // Copie de l'image originale
            imageFile.copyTo(originalDebugFile, overwrite = true)
            println("🔍 [DEBUG] Image originale sauvegardée: ${originalDebugFile.absolutePath}")
            println("🔍 [DEBUG] Taille image originale: ${originalDebugFile.length()} bytes")
            
            // 2. Traitement (OCR + Analyse structurelle)
            GridAnalyzer.preprocessImage(imageFile)
            
            // Copie de l'image prétraitée
            imageFile.copyTo(preprocessedDebugFile, overwrite = true)
            println("🔍 [DEBUG] Image prétraitée sauvegardée: ${preprocessedDebugFile.absolutePath}")
            println("🔍 [DEBUG] Taille image prétraitée: ${preprocessedDebugFile.length()} bytes")
            // ==== FIN DEBUG ====

            val grid = withContext(Dispatchers.IO) {
                gridAnalyzer.analyzeGrid(imageFile)
            }
            
            val duration = System.currentTimeMillis() - startTime
            println("✅ [/analyze] Terminé en ${duration}ms - Grille ${grid.width}x${grid.height}, ${grid.words.size} mots détectés")

            // 3. Réponse avec la structure (incluant start/end pour ton app mobile)
            call.respond(GridResponse(
                height = grid.height,
                width = grid.width,
                x = grid.x,
                y = grid.y,
                blackCells = grid.blackCells,
                // On mappe bien les WordEntry vers WordInfo pour le JSON
                words = grid.words.map {
                    WordInfo(
                        number = it.number,
                        order = it.order,
                        direction = it.direction,
                        size = it.size,
                        start = it.start, // Important pour ton front
                        end = it.end,     // Important pour ton front
                        clue = it.clue
                    )
                }
            ))

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur d'analyse", e.message))
        } finally {
            imageFile?.delete()
        }
    }

    /**
     * ÉTAPE 2 : Résolution en streaming (SSE)
     * Reçoit le JSON de la grille -> Renvoie les événements 'round' et 'complete'
     */
    post("/solve-stream") {
        try {

            val gridJson = call.receiveText()
            // On configure le Json pour ignorer les clés inconnues au cas où
            val jsonConfig = Json { ignoreUnknownKeys = true }
            val grid = jsonConfig.decodeFromString<CrosswordGrid>(gridJson)

            val apiKey = System.getenv("GOOGLE_API_KEY")
                ?: throw IllegalStateException("GOOGLE_API_KEY manquante")

            val solver = CrosswordSolver(apiKey)

            // 2. On ouvre le flux SSE (Server-Sent Events)
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {

                write("event: connected\n")
                write("data: {\"status\": \"started\"}\n\n")
                flush()

                // Cette callback est appelée à chaque fin de round par le solver
                solver.solve(grid) { round, solvedWords ->
                    // Création de l'événement
                    val roundEvent = RoundResultEvent(
                        round = round,
                        solved = solvedWords.size,
                        total = grid.getTotalCount(),
                        words = solvedWords.map {
                            WordAnswer(it.number, it.order, it.direction, it.answer ?: "")
                        }
                    )

                    // Envoi au format SSE
                    val jsonEvent = Json.encodeToString(roundEvent)
                    write("event: round\n")
                    write("data: $jsonEvent\n\n")
                    flush() // Force l'envoi immédiat
                }

                // 3. Événement de fin
                val finalEvent = FinalResultEvent(
                    rounds = 0,
                    solved = grid.getSolvedCount(),
                    total = grid.getTotalCount(),
                    words = grid.words.filter { it.isSolved() }.map {
                        WordAnswer(it.number, it.order, it.direction, it.answer ?: "")
                    }
                )
                val finalJson = Json.encodeToString(finalEvent)
                write("event: complete\n")
                write("data: $finalJson\n\n")
                flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur de résolution", e.message))
        }
    }
}