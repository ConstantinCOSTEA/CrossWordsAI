package com.controllers.grid

import com.schemas.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.io.File

class GridController {
    
    private val analyzer = GridAnalyzer()
    private val questionExtractor = SimpleQuestionExtractor()  // Utilise l'API cloud au lieu de Tesseract local
    
    /**
     * Analyse une grille de mots croisés à partir d'une image uploadée
     */
    suspend fun analyzeGrid(call: ApplicationCall) {
        val multipart = call.receiveMultipart()
        var file: File? = null

        try {
            // Récupérer le fichier uploadé
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val uploaded = File("received.png")
                    part.streamProvider().use { input ->
                        uploaded.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file = uploaded
                }
                part.dispose()
            }

            if (file == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file received"))
                return
            }

            // Analyser la grille avec OpenCV
            val gridStructure = analyzer.analyzeGrid(file!!)
            
            // Construire la réponse
            val result = GridResponse(
                words = gridStructure.words.map { word ->
                    WordInfo(
                        number = word.number,
                        size = word.length,
                        direction = word.direction,
                        crossings = word.crossings.map { crossing ->
                            CrossingInfo(
                                position = crossing.position,
                                crossingWordNumber = crossing.crossingWordNumber
                            )
                        }
                    )
                },
                annotatedImageUrl = gridStructure.annotatedImagePath?.let { "/images/$it" }
            )

            call.respond(result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError, 
                ErrorResponse(e.message ?: "Unknown error")
            )
        } finally {
            // Nettoyer le fichier temporaire
            file?.delete()
        }
    }
    
    /**
     * Extrait les questions d'une image uploadée avec OCR
     */
    suspend fun extractQuestions(call: ApplicationCall) {
        val multipart = call.receiveMultipart()
        var file: File? = null

        try {
            // Récupérer le fichier uploadé
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val uploaded = File("questions_image.png")
                    part.streamProvider().use { input ->
                        uploaded.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file = uploaded
                }
                part.dispose()
            }

            if (file == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file received"))
                return
            }

            // Extraire les questions avec OCR API
            val extractedQuestions = questionExtractor.extractQuestionsWithAPI(file!!)
            
            // Construire la réponse avec le texte brut
            val result = QuestionsResponse(
                rawText = extractedQuestions.firstOrNull()?.text,  // Texte brut complet
                questions = null  // Pas de parsing pour l'instant, on laisse le client faire
            )

            call.respond(result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError, 
                ErrorResponse(e.message ?: "Unknown error")
            )
        } finally {
            // Nettoyer le fichier temporaire
            file?.delete()
        }
    }
    
    /**
     * Teste l'OCR et retourne le texte brut (debug)
     */
    suspend fun testOCR(call: ApplicationCall) {
        val multipart = call.receiveMultipart()
        var file: File? = null

        try {
            // Récupérer le fichier uploadé
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val uploaded = File("test_ocr.png")
                    part.streamProvider().use { input ->
                        uploaded.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file = uploaded
                }
                part.dispose()
            }

            if (file == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file received"))
                return
            }

            // Tester l'OCR brut
            println("🧪 TEST OCR - Début")
            val extractedQuestions = questionExtractor.extractQuestionsWithAPI(file!!)
            println("🧪 TEST OCR - Fin: ${extractedQuestions.size} questions trouvées")
            
            // Retourner un résultat de debug
            call.respond(mapOf(
                "success" to true,
                "questionsFound" to extractedQuestions.size,
                "rawQuestions" to extractedQuestions.map { 
                    mapOf(
                        "number" to it.number,
                        "direction" to it.direction,
                        "text" to it.text
                    )
                }
            ))
            
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError, 
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "stackTrace" to e.stackTraceToString().take(500)
                )
            )
        } finally {
            file?.delete()
        }
    }
    
    /**
     * Sert les images générées (annotated_grid.png)
     */
    suspend fun serveImage(call: ApplicationCall) {
        val filename = call.parameters["filename"]
        
        if (filename == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Filename required"))
            return
        }
        
        val file = File(filename)
        
        if (file.exists()) {
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Image not found"))
        }
    }
}

