package fr.miage.m1.crosswordsai.data.api

import fr.miage.m1.crosswordsai.data.model.CrosswordData
import fr.miage.m1.crosswordsai.data.model.FinalResultEvent
import fr.miage.m1.crosswordsai.data.model.RoundResultEvent
import fr.miage.m1.crosswordsai.data.model.SseEvent
import fr.miage.m1.crosswordsai.data.model.WordAnswersResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Service pour communiquer avec le backend CrossWordsAI
 */
class CrosswordApiService(
    private val baseUrl: String = "http://10.0.2.2:8080" // localhost pour émulateur Android
) {
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            // Timeout de 5 minutes pour les requêtes SSE longues
            requestTimeoutMillis = 300_000  // 5 minutes
            connectTimeoutMillis = 30_000   // 30 secondes pour la connexion
            socketTimeoutMillis = 300_000   // 5 minutes pour le socket (SSE)
        }
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Analyse une image de grille et retourne sa structure
     * 
     * @param imageFile L'image de la grille
     * @return La structure de la grille (dimensions, cases noires, mots)
     */
    suspend fun analyzeGrid(imageFile: File): CrosswordData {
        return client.submitFormWithBinaryData(
            url = "$baseUrl/analyze",
            formData = formData {
                append("image", imageFile.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"grid.png\"")
                })
            }
        ).body()
    }
    
    /**
     * Résout une grille de mots croisés
     * 
     * @param grid La grille avec les définitions
     * @return Les réponses pour chaque mot
     */
    suspend fun solveGrid(grid: CrosswordData): WordAnswersResponse {
        return client.post("$baseUrl/solve") {
            contentType(ContentType.Application.Json)
            setBody(grid)
        }.body()
    }
    
    /**
     * Résout une grille en streaming via SSE (Server-Sent Events)
     * Les mots sont retournés progressivement au fur et à mesure de la résolution
     * 
     * @param grid La grille avec les définitions
     * @return Un Flow d'événements SSE contenant les mots résolus
     */
    fun solveGridStream(grid: CrosswordData): Flow<SseEvent> = flow {
        val gridJson = json.encodeToString(CrosswordData.serializer(), grid)
        
        client.preparePost("$baseUrl/solve-stream") {
            contentType(ContentType.Application.Json)
            setBody(gridJson)
        }.execute { response ->
            val channel: ByteReadChannel = response.bodyAsChannel()
            
            var currentEvent = ""
            var currentData = ""
            
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                
                when {
                    line.startsWith("event:") -> {
                        currentEvent = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        currentData = line.removePrefix("data:").trim()
                    }
                    line.isEmpty() && currentEvent.isNotEmpty() && currentData.isNotEmpty() -> {
                        // Fin de l'événement SSE, on le parse et l'émet
                        val event = parseSseEvent(currentEvent, currentData)
                        if (event != null) {
                            emit(event)
                        }
                        currentEvent = ""
                        currentData = ""
                    }
                }
            }
        }
    }
    
    /**
     * Parse un événement SSE brut en objet SseEvent
     */
    private fun parseSseEvent(eventType: String, data: String): SseEvent? {
        return try {
            when (eventType) {
                "connected" -> SseEvent.Connected("connected")
                "round" -> {
                    val roundEvent = json.decodeFromString<RoundResultEvent>(data)
                    SseEvent.Round(roundEvent)
                }
                "complete" -> {
                    val finalEvent = json.decodeFromString<FinalResultEvent>(data)
                    SseEvent.Complete(finalEvent)
                }
                else -> null
            }
        } catch (e: Exception) {
            SseEvent.Error("Erreur parsing SSE: ${e.message}")
        }
    }
    
    /**
     * Analyse et résout une grille en une seule requête
     * 
     * @param gridImageFile Image de la grille
     * @param cluesImageFile Image des définitions (optionnel)
     * @return Les réponses pour chaque mot
     */
    suspend fun analyzeAndSolve(
        gridImageFile: File,
        cluesImageFile: File? = null
    ): WordAnswersResponse {
        return client.submitFormWithBinaryData(
            url = "$baseUrl/analyze-and-solve",
            formData = formData {
                append("grid_image", gridImageFile.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"grid.png\"")
                })
                
                cluesImageFile?.let { file ->
                    append("clues_image", file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"clues.png\"")
                    })
                }
            }
        ).body()
    }
    
    /**
     * Vérifie si le backend est accessible
     */
    suspend fun healthCheck(): Boolean {
        return try {
            val response: Map<String, String> = client.get("$baseUrl/health").body()
            response["status"] == "UP"
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Ferme le client HTTP
     */
    fun close() {
        client.close()
    }
    
    companion object {
        // Singleton pour réutiliser le client
        @Volatile
        private var instance: CrosswordApiService? = null
        
        fun getInstance(baseUrl: String = "http://10.0.2.2:8080"): CrosswordApiService {
            return instance ?: synchronized(this) {
                instance ?: CrosswordApiService(baseUrl).also { instance = it }
            }
        }
    }
}

