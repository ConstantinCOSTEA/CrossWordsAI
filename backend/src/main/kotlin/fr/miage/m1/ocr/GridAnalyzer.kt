package fr.miage.m1.ocr

import fr.miage.m1.model.BlackCell
import fr.miage.m1.model.CrosswordGrid
import fr.miage.m1.model.Direction
import fr.miage.m1.model.WordEntry
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgcodecs.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.*
import java.io.File

class GridAnalyzer(
    private val ocrApiKey: String? = null
) {
    private val clueExtractor = ClueExtractor()

    companion object {
        /**
         * Prétraitement optimisé : Redimensionnement + Netteté + Binarisation
         */
        fun preprocessImage(imageFile: File) {
            val src = imread(imageFile.absolutePath)
            if (src.empty()) return

            // 1. REDIMENSIONNEMENT (Max 1500px pour vitesse & anti-timeout)
            val maxDim = 1500.0
            val scale = if (src.cols() > maxDim || src.rows() > maxDim) {
                minOf(maxDim / src.cols(), maxDim / src.rows())
            } else {
                1.0
            }

            val resized = Mat()
            if (scale < 1.0) {
                resize(src, resized, Size(), scale, scale, INTER_AREA)
            } else {
                src.copyTo(resized)
            }

            // 2. Conversion Gris
            val gray = Mat()
            cvtColor(resized, gray, COLOR_BGR2GRAY)

            // 3. Netteté (Sharpen)
            val kernelData = floatArrayOf(0f, -1f, 0f, -1f, 5f, -1f, 0f, -1f, 0f)
            val kernel = Mat(3, 3, CV_32F, FloatPointer(*kernelData))
            filter2D(gray, gray, -1, kernel)

            // 4. Binarisation
            threshold(gray, gray, 200.0, 255.0, THRESH_BINARY)

            imwrite(imageFile.absolutePath, gray)

            src.release(); resized.release(); gray.release(); kernel.release()
        }
    }

    fun analyzeGrid(imageFile: File): CrosswordGrid {
        val image = imread(imageFile.absolutePath, IMREAD_GRAYSCALE)

        try {
            // --- 1. OCR & Parsing ---
            val rawText = clueExtractor.extractText(imageFile, ocrApiKey)
            
            // DEBUG: Afficher le texte brut de l'OCR
            println("🔍 [OCR DEBUG] Texte brut extrait (${rawText.length} caractères):")
            println("------- DÉBUT OCR -------")
            println(rawText.take(1000)) // Limité à 1000 caractères pour éviter le spam
            if (rawText.length > 1000) println("... (tronqué)")
            println("------- FIN OCR -------")
            
            val parsedClues = parseCluesImproved(rawText)
            
            // DEBUG: Afficher les clues détectées
            println("🔍 [PARSE DEBUG] Indices horizontaux détectés: ${parsedClues.horizontal.keys.sorted()}")
            println("🔍 [PARSE DEBUG] Indices verticaux détectés: ${parsedClues.vertical.keys.sorted()}")

            // Dimensions génériques basées sur les clés détectées
            // Pour les lignes : on prend le max des identifiants horizontaux
            // Pour les colonnes : on prend le max des identifiants verticaux
            val numRows = parsedClues.horizontal.keys.maxOrNull() ?: 1
            val numCols = parsedClues.vertical.keys.maxOrNull() ?: 1
            
            // DEBUG: Afficher les dimensions calculées
            println("🔍 [GRID DEBUG] Dimensions calculées: ${numCols}x${numRows} (cols x rows)")
            if (numRows == 1 || numCols == 1) {
                println("⚠️ [WARNING] Grille 1x1 détectée - L'OCR n'a probablement pas trouvé de définitions!")
            }

            // --- 2. Détection Grille ---
            val gridRect = findGridBounds(image)
            val cellWidth = gridRect.width() / numCols
            val cellHeight = gridRect.height() / numRows
            val startX = gridRect.x()
            val startY = gridRect.y()

            // --- 3. Scan Cases ---
            val cellMatrix = Array(numRows) { Array(numCols) { true } }
            val coloredCells = mutableListOf<BlackCell>()

            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val cellX = startX + col * cellWidth
                    val cellY = startY + row * cellHeight

                    val blackPercent = analyzeBlackPercentage(image, cellX, cellY, cellWidth, cellHeight)

                    if (blackPercent > 50) { // Case noire
                        cellMatrix[row][col] = false
                        coloredCells.add(BlackCell(col + 1, row + 1))
                    }
                }
            }

            // --- 4. Construction ---
            val words = buildWordsFromMatrix(cellMatrix, numRows, numCols, parsedClues)
            val grid = CrosswordGrid.create(numCols, numRows, coloredCells, "letters", "numbers")
            grid.words.addAll(words)

            return grid

        } finally {
            image.release()
        }
    }

    private fun parseCluesImproved(text: String): ParsedClues {
        val horizontal = mutableMapOf<Int, MutableList<String>>()
        val vertical = mutableMapOf<Int, MutableList<String>>()

        val lines = text.replace("\r\n", "\n").replace("\r", "\n").lines()

        var inHorizontal = false
        var inVertical = false
        var currentKey = 0
        val currentDef = StringBuilder()

        // Regex flexible pour détecter le début d'une définition
        // Accepte: chiffres ou lettres suivis de séparateurs
        val startIdentifierRegex = Regex("""^([A-Z]|\d+)(\s*[.:\-)\]]|\s).*""")

        fun saveCurrent() {
            if (currentDef.isBlank()) return

            val fullText = cleanClueText(currentDef.toString())

            if (fullText.isNotBlank()) {
                val splitDefs = splitIntoMultipleClues(fullText)
                val cleanDefs = splitDefs.map { cleanClueText(it) }

                if (inHorizontal && currentKey > 0) {
                    horizontal.getOrPut(currentKey) { mutableListOf() }.addAll(cleanDefs)
                }
                if (inVertical && currentKey > 0) {
                    vertical.getOrPut(currentKey) { mutableListOf() }.addAll(cleanDefs)
                }
            }
            currentDef.clear()
        }

        fun parseIdentifier(text: String): Int? {
            // Essaie d'extraire un chiffre
            val numMatch = Regex("""^(\d+)""").find(text)
            if (numMatch != null) {
                return numMatch.groupValues[1].toIntOrNull()
            }

            // Essaie d'extraire une lettre majuscule et la convertir en index
            val letterMatch = Regex("""^([A-Z])""").find(text)
            if (letterMatch != null) {
                return letterMatch.groupValues[1][0] - 'A' + 1
            }

            return null
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // --- Détection des TITRES ---
            if (trimmed.uppercase().contains("HORIZONTALEMENT") || trimmed.uppercase().contains("HORIZONTAL")) {
                saveCurrent()
                inHorizontal = true
                inVertical = false
                currentKey = 0
                continue
            }
            if (trimmed.uppercase().contains("VERTICALEMENT") || trimmed.uppercase().contains("VERTICAL")) {
                saveCurrent()
                inVertical = true
                inHorizontal = false
                currentKey = 0
                continue
            }
            if (!inHorizontal && !inVertical) continue

            // Ignore les lignes parasites
            if (trimmed.matches(Regex("""^[\d.A-Z\s]+$""")) && trimmed.length < 3) continue

            // --- Détection NOUVELLE DÉFINITION ---
            if (trimmed.matches(startIdentifierRegex)) {
                saveCurrent()

                val identifier = parseIdentifier(trimmed)
                if (identifier != null) {
                    currentKey = identifier
                } else {
                    currentKey++ // Incrémentation automatique si on ne trouve pas d'identifiant
                }

                currentDef.append(trimmed)
            } else {
                // Suite de la définition précédente (multiligne)
                if (currentDef.isNotEmpty()) currentDef.append(" ")
                currentDef.append(trimmed)
            }
        }
        saveCurrent() // Sauvegarder la toute dernière

        return ParsedClues(horizontal, vertical)
    }

    /**
     * Nettoie le début d'une définition de manière ROBUSTE.
     * Enlève l'Identifiant (Numéro/Lettre) + Séparateur
     */
    private fun cleanClueText(text: String): String {
        return text.replace(Regex("""^([A-Z]|\d+)(\s*[.:\-)\]]\s*|\s+)"""), "").trim()
    }

    /**
     * Découpe une ligne contenant plusieurs définitions séparées par ponctuation.
     */
    private fun splitIntoMultipleClues(text: String): List<String> {
        val parts = text.split(Regex("""(?<=[.?!])\s+(?=[A-ZÀ-ÖØ-Þ])"""))
        return parts.map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun findGridBounds(binaryImage: Mat): Rect {
        val contours = MatVector()
        val edges = Mat()
        Canny(binaryImage, edges, 50.0, 200.0)
        findContours(edges, contours, Mat(), RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

        var bestRect = Rect(0, 0, binaryImage.cols(), binaryImage.rows())
        var maxArea = 0.0
        val imageArea = (binaryImage.cols() * binaryImage.rows()).toDouble()

        for (i in 0 until contours.size()) {
            val rect = boundingRect(contours[i])
            val area = rect.area().toDouble()
            if (area > maxArea && area > imageArea * 0.1 && area < imageArea * 0.95) {
                val ratio = rect.width().toDouble() / rect.height().toDouble()
                if (ratio in 0.5..2.0) {
                    maxArea = area
                    bestRect = rect
                }
            }
        }
        edges.release()
        return bestRect
    }

    private fun analyzeBlackPercentage(binaryImage: Mat, cellX: Int, cellY: Int, w: Int, h: Int): Int {
        val marginX = (w * 0.25).toInt()
        val marginY = (h * 0.25).toInt()
        val x1 = (cellX + marginX).coerceIn(0, binaryImage.cols() - 1)
        val y1 = (cellY + marginY).coerceIn(0, binaryImage.rows() - 1)
        val x2 = (cellX + w - marginX).coerceIn(0, binaryImage.cols() - 1)
        val y2 = (cellY + h - marginY).coerceIn(0, binaryImage.rows() - 1)

        if (x2 <= x1 || y2 <= y1) return 0
        var blackPixels = 0
        var totalPixels = 0
        val step = 2

        for (y in y1 until y2 step step) {
            for (x in x1 until x2 step step) {
                if ((binaryImage.ptr(y, x).get(0).toInt() and 0xFF) == 0) blackPixels++
                totalPixels++
            }
        }
        return if (totalPixels > 0) (blackPixels * 100) / totalPixels else 0
    }

    private fun buildWordsFromMatrix(cellMatrix: Array<Array<Boolean>>, numRows: Int, numCols: Int, clues: ParsedClues): List<WordEntry> {
        val words = mutableListOf<WordEntry>()

        // --- Horizontal ---
        for (row in 0 until numRows) {
            val lineClues = clues.horizontal[row + 1] ?: listOf("")
            var currentOrder = 0 // Compteur de mots sur la ligne
            var startCol = -1

            for (col in 0..numCols) {
                val isWhite = col < numCols && cellMatrix[row][col]

                if (isWhite && startCol == -1) {
                    startCol = col // Début trouvé
                } else if (!isWhite && startCol != -1) {
                    // Fin trouvée (Case noire ou bord)
                    val size = col - startCol

                    // On garde la sécurité pour ne pas créer de mots d'une seule lettre
                    if (size >= 2) {
                        currentOrder++
                        val clueText = lineClues.getOrElse(currentOrder - 1) { "" }

                        words.add(WordEntry(
                            number = row + 1,
                            order = currentOrder,
                            direction = Direction.HORIZONTAL,
                            size = size,
                            start = startCol + 1, // Conversion Base 1
                            end = col,            // 'col' est l'index de la case noire, donc la fin du mot en Base 1
                            clue = clueText
                        ))
                    }
                    startCol = -1
                }
            }
        }

        // --- Vertical ---
        for (col in 0 until numCols) {
            val colClues = clues.vertical[col + 1] ?: listOf("")
            var currentOrder = 0
            var startRow = -1

            for (row in 0..numRows) {
                val isWhite = row < numRows && cellMatrix[row][col]

                if (isWhite && startRow == -1) {
                    startRow = row
                } else if (!isWhite && startRow != -1) {
                    val size = row - startRow

                    if (size >= 2) {
                        currentOrder++
                        val clueText = colClues.getOrElse(currentOrder - 1) { "" }

                        words.add(WordEntry(
                            number = col + 1,
                            order = currentOrder,
                            direction = Direction.VERTICAL,
                            size = size,
                            start = startRow + 1,
                            end = row,
                            clue = clueText
                        ))
                    }
                    startRow = -1
                }
            }
        }
        return words
    }

    private data class ParsedClues(
        val horizontal: Map<Int, List<String>>,
        val vertical: Map<Int, List<String>>
    )
}