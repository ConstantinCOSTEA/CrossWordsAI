package fr.miage.m1.routes

import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.Direction
import fr.miage.m1.ocr.ClueExtractor
import fr.miage.m1.ocr.GridAnalyzer
import fr.miage.m1.schemas.*
import fr.miage.m1.solver.CrosswordSolver
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

fun Route.crosswordRoutes() {
    val gridAnalyzer = GridAnalyzer()
    val clueExtractor = ClueExtractor()

    post("/analyze") {
        var imageFile: File? = null
        try {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") {
                    val fileName = "grid_${UUID.randomUUID()}.png"
                    imageFile = File(System.getProperty("java.io.tmpdir"), fileName)
                    part.provider().toInputStream().use { input -> imageFile.outputStream().use { output -> input.copyTo(output) } }
                }
                part.dispose()
            }

            if (imageFile == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Aucune image reçue"))
                return@post
            }

            // Pré-traitement de l'image
            GridAnalyzer.preprocessImage(imageFile)

            val grid = withContext(Dispatchers.IO) {
                gridAnalyzer.analyzeGrid(imageFile)
            }

            call.respond(GridResponse(
                height = grid.height,
                width = grid.width,
                x = grid.x,
                y = grid.y,
                blackCells = grid.blackCells,
                words = grid.words.map { WordInfo(it.number, it.order, it.direction, it.size, it.clue) }
            ))

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur d'analyse", e.message))
        } finally {
            imageFile?.delete()
        }
    }

    post("/extract-clues") {
        var imageFile: File? = null
        try {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") {
                    val fileName = "clues_${UUID.randomUUID()}.png"
                    imageFile = File(System.getProperty("java.io.tmpdir"), fileName)
                    part.provider().toInputStream().use { input -> imageFile.outputStream().use { output -> input.copyTo(output) } }
                }
                part.dispose()
            }

            if (imageFile == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Aucune image"))
                return@post
            }

            GridAnalyzer.preprocessImage(imageFile)

            val clues = withContext(Dispatchers.IO) { clueExtractor.extractClues(imageFile) }
            val cluesMap = clues.map { (key, value) -> "${key.number}_${key.direction}" to value }.toMap()
            call.respond(mapOf("clues" to cluesMap))

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur OCR", e.message))
        } finally {
            imageFile?.delete()
        }
    }

    post("/solve") {
        try {
            val grid = call.receive<CrosswordGrid>()
            call.solveAndRespond(grid)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur", e.message))
        }
    }

    post("/analyze-and-solve") {
        var gridImageFile: File? = null
        var cluesImageFile: File? = null
        try {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    when (part.name) {
                        "grid_image" -> {
                            gridImageFile = File(System.getProperty("java.io.tmpdir"), "grid_${UUID.randomUUID()}.png")
                            part.provider().toInputStream().use { input -> gridImageFile.outputStream().use { output -> input.copyTo(output) } }
                        }
                        "clues_image" -> {
                            cluesImageFile = File(System.getProperty("java.io.tmpdir"), "clues_${UUID.randomUUID()}.png")
                            part.provider().toInputStream().use { input -> cluesImageFile.outputStream().use { output -> input.copyTo(output) } }
                        }
                    }
                }
                part.dispose()
            }

            if (gridImageFile == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Image manquante"))
                return@post
            }

            GridAnalyzer.preprocessImage(gridImageFile)
            if (cluesImageFile != null) GridAnalyzer.preprocessImage(cluesImageFile)

            val grid = withContext(Dispatchers.IO) { gridAnalyzer.analyzeGrid(gridImageFile) }

            if (cluesImageFile != null) {
                val clues = withContext(Dispatchers.IO) { clueExtractor.extractClues(cluesImageFile) }
                grid.words.forEach { word ->
                    val direction = if (word.direction == Direction.HORIZONTAL) "horizontal" else "vertical"
                    val key = ClueExtractor.ClueKey(word.number, direction)
                    clues[key]?.let { clue -> grid.words[grid.words.indexOf(word)] = word.copy(clue = clue) }
                }
            }

            call.solveAndRespond(grid)

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erreur", e.message))
        } finally {
            gridImageFile?.delete()
            cluesImageFile?.delete()
        }
    }
}

/**
 * Fonction d'extension pour résoudre une grille et répondre avec les réponses
 */
private suspend fun ApplicationCall.solveAndRespond(grid: CrosswordGrid) {
    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: throw IllegalStateException("GOOGLE_API_KEY manquante dans les variables d'environnement")

    val solver = CrosswordSolver(apiKey)
    val solvedGrid = solver.solve(grid)

    val answers = solvedGrid.words
        .filter { it.isSolved() }
        .map { WordAnswer(it.number, it.order, it.direction, it.answer!!) }

    respond(AnswersResponse(words = answers))
}