package fr.miage.m1.controllers.grid

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.leptonica.PIX
import org.bytedeco.leptonica.global.leptonica.*
import org.bytedeco.tesseract.TessBaseAPI
import java.io.File

/**
 * Service d'extraction de questions depuis une image avec OCR
 */
class QuestionExtractor {
    
    data class ExtractedQuestion(
        val number: Int?,
        val direction: String?, // "horizontal" ou "vertical"
        val text: String
    )
    
    /**
     * Extrait toutes les questions d'une image de grille de mots croisés
     */
    fun extractQuestions(imageFile: File): List<ExtractedQuestion> {
        println("🔍 Extraction des questions avec OCR...")
        
        val api = TessBaseAPI()
        
        try {
            // Chercher le chemin des données Tesseract
            val tessdataPaths = listOf(
                "/opt/homebrew/share/tessdata",
                "/usr/local/share/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                System.getenv("TESSDATA_PREFIX")
            ).filterNotNull()
            
            var initialized = false
            var dataPath: String? = null
            
            // Essayer chaque chemin
            for (path in tessdataPaths) {
                val pathFile = File(path)
                if (pathFile.exists()) {
                    println("📁 Tentative avec tessdata: $path")
                    if (api.Init(path, "eng") == 0) {
                        initialized = true
                        dataPath = path
                        println("✅ Tesseract initialisé avec: $path")
                        break
                    }
                }
            }
            
            if (!initialized) {
                // Dernier essai sans chemin spécifique
                if (api.Init(null, "eng") == 0) {
                    initialized = true
                    println("✅ Tesseract initialisé (chemin par défaut)")
                }
            }
            
            if (!initialized) {
                throw RuntimeException("""
                    Impossible d'initialiser Tesseract OCR.
                    
                    Veuillez installer Tesseract:
                    macOS: brew install tesseract tesseract-lang
                    Linux: sudo apt-get install tesseract-ocr tesseract-ocr-eng
                    
                    Chemins testés: ${tessdataPaths.joinToString(", ")}
                """.trimIndent())
            }
            
            // Charger l'image avec Leptonica
            val image: PIX = pixRead(imageFile.absolutePath)
            api.SetImage(image)
            
            // Extraire le texte
            val outText: BytePointer = api.GetUTF8Text()
            val extractedText = outText.string
            
            println("📝 Texte extrait :")
            println(extractedText)
            
            // Parser le texte pour identifier les questions
            val questions = parseQuestionsFromText(extractedText)
            
            // Libérer la mémoire
            outText.deallocate()
            pixDestroy(image)
            api.End()
            
            println("✅ ${questions.size} questions extraites")
            
            return questions
            
        } catch (e: Exception) {
            e.printStackTrace()
            api.End()
            throw RuntimeException("Erreur lors de l'extraction OCR: ${e.message}")
        }
    }
    
    /**
     * Parse le texte brut pour identifier les questions
     * Format attendu : "1. Horizontal: Question...", "2. Vertical: Question..."
     */
    private fun parseQuestionsFromText(text: String): List<ExtractedQuestion> {
        val questions = mutableListOf<ExtractedQuestion>()
        
        // Regex pour détecter les patterns de questions
        // Exemples: "1. Horizontal:", "1 - Horizontal", "Horizontal 1:", etc.
        val patterns = listOf(
            Regex("""(\d+)\s*[.:-]\s*(Horizontal|Vertical|H|V)\s*[.:-]\s*(.+?)(?=\n\d+|${'$'})""", RegexOption.IGNORE_CASE),
            Regex("""(Horizontal|Vertical|H|V)\s+(\d+)\s*[.:-]\s*(.+?)(?=\n|${'$'})""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*\.\s*(.+?)(?=\n\d+|${'$'})""")  // Numéro simple suivi du texte
        )
        
        for (pattern in patterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                try {
                    val groups = match.groupValues
                    
                    when (groups.size) {
                        4 -> {
                            // Format: "1. Horizontal: Question"
                            val number = groups[1].toIntOrNull()
                            val direction = normalizeDirection(groups[2])
                            val questionText = groups[3].trim()
                            
                            if (questionText.isNotBlank()) {
                                questions.add(ExtractedQuestion(number, direction, questionText))
                            }
                        }
                        3 -> {
                            // Format: "1. Question" (sans direction)
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
            
            if (questions.isNotEmpty()) break // Si on a trouvé des questions, on arrête
        }
        
        return questions
    }
    
    /**
     * Normalise la direction en "horizontal" ou "vertical"
     */
    private fun normalizeDirection(dir: String): String {
        return when (dir.lowercase().first()) {
            'h' -> "horizontal"
            'v' -> "vertical"
            else -> dir.lowercase()
        }
    }
}

