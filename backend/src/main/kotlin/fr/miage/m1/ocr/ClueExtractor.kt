package fr.miage.m1.ocr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.*

class ClueExtractor {

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
        @SerialName("ErrorMessage") val errorMessage: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extractText(imageFile: File, apiKey: String? = null): String {
        val imageBytes = imageFile.readBytes()
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)
        return callOCRSpaceAPI(base64Image, apiKey)
    }

    fun extractClues(imageFile: File, apiKey: String? = null): Map<ClueKey, String> {
        val rawText = extractText(imageFile, apiKey)
        return parseCluesFromText(rawText)
    }

    private fun callOCRSpaceAPI(base64Image: String, apiKey: String?): String {
        val url = URI.create("https://api.ocr.space/parse/image").toURL()
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val base64WithPrefix = "data:image/png;base64,$base64Image"
            val encodedImage = java.net.URLEncoder.encode(base64WithPrefix, "UTF-8")
            val params = "base64Image=$encodedImage&language=fre&isOverlayRequired=false&apikey=${apiKey ?: "helloworld"}"

            connection.outputStream.use { it.write(params.toByteArray()) }

            if (connection.responseCode != 200) throw RuntimeException("API Error: ${connection.responseCode}")
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            val obj = json.decodeFromString<OCRSpaceResponse>(response)
            if (obj.isErroredOnProcessing) throw RuntimeException("OCR Error: ${obj.errorMessage}")
            return obj.parsedResults?.firstOrNull()?.parsedText ?: ""

        } finally {
            connection.disconnect()
        }
    }

    private fun parseCluesFromText(text: String): Map<ClueKey, String> {
        val clues = mutableMapOf<ClueKey, String>()

        // Regex pour capturer: (Numero) . (Texte)
        val simplePattern = Regex("""(\d+)\s*[.:\-]\s*(.+?)(?=\n\d+|$)""", RegexOption.DOT_MATCHES_ALL)

        val horizRegex = Regex("""HORIZONTAL(?:EMENT)?[:\s]*(.+?)(?=VERTICAL|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val vertRegex = Regex("""VERTICAL(?:EMENT)?[:\s]*(.+?)$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        fun addClues(sectionText: String, direction: String) {
            simplePattern.findAll(sectionText).forEach { m ->
                val number = m.groupValues[1].toIntOrNull()
                val definition = m.groupValues[2].trim()
                if (number != null && definition.isNotBlank()) {
                    clues[ClueKey(number, direction)] = definition
                }
            }
        }

        val hMatch = horizRegex.find(text)
        val vMatch = vertRegex.find(text)

        if (hMatch != null) addClues(hMatch.groupValues[1], "horizontal")
        if (vMatch != null) addClues(vMatch.groupValues[1], "vertical")

        if (clues.isEmpty()) addClues(text, "horizontal") // Fallback

        return clues
    }

    data class ClueKey(val number: Int, val direction: String)
}