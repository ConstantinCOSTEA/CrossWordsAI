package com.controllers.grid

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString

/**
 * Extracteur de questions utilisant une API OCR externe
 * Alternative à Tesseract qui fonctionne sans installation locale
 */
class SimpleQuestionExtractor {
    
    data class ExtractedQuestion(
        val number: Int?,
        val direction: String?,
        val text: String
    )
    
    @Serializable
    private data class OCRSpaceResponse(
        @SerialName("ParsedResults") val parsedResults: List<ParsedResult>? = null,
        @SerialName("OCRExitCode") val ocrExitCode: Int = 0,
        @SerialName("IsErroredOnProcessing") val isErroredOnProcessing: Boolean = false,
        @SerialName("ErrorMessage") val errorMessage: List<String>? = null
    )
    
    @Serializable
    private data class ParsedResult(
        @SerialName("ParsedText") val parsedText: String = "",
        @SerialName("ErrorMessage") val errorMessage: String? = null,
        @SerialName("FileParseExitCode") val fileParseExitCode: Int = 0
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Extrait les questions en utilisant OCR.space API (gratuite)
     * Alternative : Google Cloud Vision, Azure Computer Vision, AWS Textract
     */
    fun extractQuestionsWithAPI(imageFile: File, apiKey: String? = null): List<ExtractedQuestion> {
        println("🔍 Extraction des questions avec OCR.space API...")
        
        try {
            // Encoder l'image en base64
            val imageBytes = imageFile.readBytes()
            val base64Image = Base64.getEncoder().encodeToString(imageBytes)
            
            // Appeler l'API OCR.space (gratuite, pas besoin de clé pour usage basique)
            val extractedText = callOCRSpaceAPI(base64Image, apiKey)
            
            println("📝 Texte extrait :")
            println(extractedText)
            
            // NOUVEAU : Retourner tout le texte brut sans filtrage
            // Chaque cas est différent, donc on laisse le client parser
            val allText = extractedText.trim()
            
            println("✅ Texte extrait avec succès (${allText.length} caractères)")
            
            // Retourner le texte brut comme une seule "question"
            return listOf(
                ExtractedQuestion(
                    number = null,
                    direction = null,
                    text = allText
                )
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Erreur lors de l'extraction OCR: ${e.message}")
        }
    }
    
    /**
     * Appelle l'API OCR.space pour extraire le texte
     * API gratuite : https://ocr.space/ocrapi
     */
    private fun callOCRSpaceAPI(base64Image: String, apiKey: String?): String {
        val url = URL("https://api.ocr.space/parse/image")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            // Encoder correctement l'image en base64 avec le bon format
            val base64WithPrefix = "data:image/png;base64,$base64Image"
            val encodedImage = java.net.URLEncoder.encode(base64WithPrefix, "UTF-8")
            
            // Construire le body de la requête
            val params = buildString {
                append("base64Image=").append(encodedImage)
                append("&language=eng")
                append("&isOverlayRequired=false")
                if (apiKey != null) {
                    append("&apikey=").append(apiKey)
                } else {
                    // Clé publique pour tests (limitée à 25,000 requêtes/mois)
                    append("&apikey=helloworld")
                }
            }
            
            connection.outputStream.use { os ->
                os.write(params.toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw RuntimeException("API OCR.space a retourné le code: $responseCode")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Parser la réponse JSON
            return parseOCRSpaceResponse(response)
            
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Parse la réponse de OCR.space
     */
    private fun parseOCRSpaceResponse(jsonResponse: String): String {
        println("📄 Réponse brute de l'API:")
        println(jsonResponse.take(500)) // Afficher les premiers 500 caractères
        
        try {
            val response = json.decodeFromString<OCRSpaceResponse>(jsonResponse)
            
            if (response.isErroredOnProcessing) {
                val errors = response.errorMessage?.joinToString(", ") ?: "Unknown error"
                throw RuntimeException("Erreur OCR API: $errors")
            }
            
            if (response.parsedResults.isNullOrEmpty()) {
                throw RuntimeException("Aucun résultat dans la réponse OCR")
            }
            
            val parsedText = response.parsedResults[0].parsedText
            
            if (parsedText.isBlank()) {
                throw RuntimeException("Texte extrait vide - l'image ne contient peut-être pas de texte lisible")
            }
            
            return parsedText
            
        } catch (e: Exception) {
            println("❌ Erreur de parsing JSON: ${e.message}")
            throw RuntimeException("Impossible de parser la réponse OCR: ${e.message}")
        }
    }
    
    /**
     * Parse le texte brut pour identifier les questions
     */
    private fun parseQuestionsFromText(text: String): List<ExtractedQuestion> {
        val questions = mutableListOf<ExtractedQuestion>()
        
        // Regex pour détecter les patterns de questions
        val patterns = listOf(
            Regex("""(\d+)\s*[.:-]\s*(Horizontal|Vertical|H|V)\s*[.:-]\s*(.+?)(?=\n\d+|${'$'})""", RegexOption.IGNORE_CASE),
            Regex("""(Horizontal|Vertical|H|V)\s+(\d+)\s*[.:-]\s*(.+?)(?=\n|${'$'})""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*\.\s*(.+?)(?=\n\d+|${'$'})""")
        )
        
        for (pattern in patterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                try {
                    val groups = match.groupValues
                    
                    when (groups.size) {
                        4 -> {
                            val number = groups[1].toIntOrNull()
                            val direction = normalizeDirection(groups[2])
                            val questionText = groups[3].trim()
                            
                            if (questionText.isNotBlank()) {
                                questions.add(ExtractedQuestion(number, direction, questionText))
                            }
                        }
                        3 -> {
                            val number = groups[1].toIntOrNull()
                            val questionText = groups[2].trim()
                            
                            if (questionText.isNotBlank()) {
                                questions.add(ExtractedQuestion(number, null, questionText))
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Erreur de parsing pour: ${match.value}")
                }
            }
            
            if (questions.isNotEmpty()) break
        }
        
        return questions
    }
    
    private fun normalizeDirection(dir: String): String {
        return when (dir.lowercase().first()) {
            'h' -> "horizontal"
            'v' -> "vertical"
            else -> dir.lowercase()
        }
    }
}

