package com.controllers.grid

import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgcodecs.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.*
import java.io.File
import kotlin.math.abs

class GridAnalyzer {
    
    data class GridCell(
        val row: Int,
        val col: Int,
        val type: String, // "white", "black", ou "empty"
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val number: Int? = null
    )
    
    data class Crossing(
        val position: Int,      // Position dans le mot actuel (1-indexed)
        val crossingWordNumber: Int  // Numéro du mot qui croise
    )
    
    data class Word(
        val number: Int,
        val direction: String, // "horizontal" ou "vertical"
        val startRow: Int,
        val startCol: Int,
        val length: Int,
        val crossings: List<Crossing> = emptyList()
    )
    
    data class GridStructure(
        val rows: Int,
        val cols: Int,
        val words: List<Word>,
        val annotatedImagePath: String? = null
    )
    
    fun analyzeGrid(imageFile: File): GridStructure {
        println("🖼️  Chargement de l'image...")
        
        // Charger l'image
        val image = imread(imageFile.absolutePath, IMREAD_COLOR)
        
        if (image.empty()) {
            throw IllegalArgumentException("Impossible de charger l'image: ${imageFile.absolutePath}")
        }
        
        val width = image.cols()
        val height = image.rows()
        println("📐 Dimensions: ${width}x${height}")
        
        // Copier l'image pour l'annotation
        val annotated = image.clone()
        
        // Convertir en niveaux de gris
        val gray = Mat()
        cvtColor(image, gray, COLOR_BGR2GRAY)
        
        // Appliquer un threshold binaire inversé
        val binary = Mat()
        threshold(gray, binary, 200.0, 255.0, THRESH_BINARY_INV)
        
        // Détecter les contours
        val contours = MatVector()
        val hierarchy = Mat()
        findContours(binary, contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE)
        
        println("🔍 Contours trouvés: ${contours.size()}")
        
        // Filtrer et dessiner les rectangles (cellules)
        val rectangles = mutableListOf<Rect>()
        var detectedCount = 0
        
        for (i in 0 until contours.size()) {
            val contour = contours.get(i)
            val area = contourArea(contour)
            
            // Filtrer par aire (ajustez ces valeurs selon votre grille)
            if (area < 500 || area > 5000) continue
            
            val rect = boundingRect(contour)
            
            // Filtrer par aspect ratio (presque carré)
            val aspectRatio = rect.width().toDouble() / rect.height().toDouble()
            if (aspectRatio < 0.7 || aspectRatio > 1.4) continue
            
            // Filtrer par taille minimale
            if (rect.width() < 30 || rect.height() < 30) continue
            
            // Dessiner un rectangle rouge autour de la cellule
            rectangle(
                annotated,
                Point(rect.x(), rect.y()),
                Point(rect.x() + rect.width(), rect.y() + rect.height()),
                Scalar(0.0, 0.0, 255.0, 0.0), // Rouge en BGR
                2, // Épaisseur
                LINE_8,
                0
            )
            
            rectangles.add(rect)
            detectedCount++
        }
        
        println("✅ Cellules détectées et dessinées: $detectedCount")
        
        // Organiser en grille
        val xPositions = rectangles.map { it.x() }.distinct().sorted()
        val yPositions = rectangles.map { it.y() }.distinct().sorted()
        
        val colPositions = clusterLines(xPositions, 15).sorted()
        val rowPositions = clusterLines(yPositions, 15).sorted()
        
        println("📏 Grille: ${rowPositions.size} lignes x ${colPositions.size} colonnes")
        
        // Construire la grille logique
        val grid = MutableList(rowPositions.size) { MutableList(colPositions.size) { "empty" } }
        val cellMap = mutableMapOf<Pair<Int, Int>, Rect>() // Map (row, col) -> Rect
        
        for (rect in rectangles) {
            val row = findClosestIndex(rowPositions, rect.y(), 20)
            val col = findClosestIndex(colPositions, rect.x(), 20)
            
            if (row != -1 && col != -1) {
                grid[row][col] = "white"
                cellMap[Pair(row, col)] = rect
            }
        }
        
        // Détecter les mots dans la grille
        val words = detectWords(grid)
        println("🔤 Mots détectés: ${words.size}")
        
        // Dessiner les numéros et flèches HORS de la grille
        for (word in words) {
            val cellKey = Pair(word.startRow, word.startCol)
            val rect = cellMap[cellKey]
            
            if (rect != null) {
                val text = word.number.toString()
                
                if (word.direction == "horizontal") {
                    // Pour horizontal : numéro et flèche À GAUCHE de la cellule
                    val textPos = Point(rect.x() - 25, rect.y() + rect.height() / 2 + 5)
                    
                    putText(
                        annotated, 
                        text, 
                        textPos, 
                        FONT_HERSHEY_SIMPLEX, 
                        0.7, 
                        Scalar(0.0, 0.0, 255.0, 0.0), // Rouge
                        2, 
                        LINE_8, 
                        false
                    )
                    
                    // Flèche horizontale → à gauche
                    val arrowStart = Point(rect.x() - 35, rect.y() + rect.height() / 2)
                    val arrowEnd = Point(rect.x() - 5, rect.y() + rect.height() / 2)
                    arrowedLine(
                        annotated, 
                        arrowStart, 
                        arrowEnd, 
                        Scalar(0.0, 0.0, 255.0, 0.0), // Rouge
                        2, 
                        LINE_8, 
                        0, 
                        0.3
                    )
                } else {
                    // Pour vertical : numéro et flèche AU-DESSUS de la cellule
                    val textPos = Point(rect.x() + rect.width() / 2 - 8, rect.y() - 10)
                    
                    putText(
                        annotated, 
                        text, 
                        textPos, 
                        FONT_HERSHEY_SIMPLEX, 
                        0.7, 
                        Scalar(0.0, 0.0, 255.0, 0.0), // Rouge
                        2, 
                        LINE_8, 
                        false
                    )
                    
                    // Flèche verticale ↓ au-dessus
                    val arrowStart = Point(rect.x() + rect.width() / 2, rect.y() - 35)
                    val arrowEnd = Point(rect.x() + rect.width() / 2, rect.y() - 5)
                    arrowedLine(
                        annotated, 
                        arrowStart, 
                        arrowEnd, 
                        Scalar(0.0, 0.0, 255.0, 0.0), // Rouge
                        2, 
                        LINE_8, 
                        0, 
                        0.3
                    )
                }
            }
        }
        
        // Sauvegarder l'image annotée
        val outputPath = "annotated_grid.png"
        imwrite(outputPath, annotated)
        println("💾 Image avec numéros et flèches sauvegardée: $outputPath")
        
        // Libérer la mémoire
        image.release()
        annotated.release()
        gray.release()
        binary.release()
        hierarchy.release()
        
        return GridStructure(
            rows = rowPositions.size,
            cols = colPositions.size,
            words = words,
            annotatedImagePath = outputPath
        )
    }
    
    private fun findClosestIndex(positions: List<Int>, target: Int, tolerance: Int): Int {
        for ((index, pos) in positions.withIndex()) {
            if (abs(pos - target) <= tolerance) {
                return index
            }
        }
        return -1
    }
    
    private fun clusterLines(lines: List<Int>, tolerance: Int): List<Int> {
        if (lines.isEmpty()) return emptyList()
        
        val sorted = lines.sorted()
        val clusters = mutableListOf<MutableList<Int>>()
        var currentCluster = mutableListOf(sorted.first())
        
        for (line in sorted.drop(1)) {
            if (line - currentCluster.last() <= tolerance) {
                currentCluster.add(line)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(line)
            }
        }
        clusters.add(currentCluster)
        
        // Retourner la médiane de chaque cluster
        return clusters.map { cluster -> cluster[cluster.size / 2] }
    }
    
    private fun findCrossings(words: List<Word>): List<Word> {
        // Pour chaque mot, trouver les croisements avec les autres mots
        return words.map { currentWord ->
            val crossings = mutableListOf<Crossing>()
            
            words.forEach { otherWord ->
                if (currentWord.number != otherWord.number && 
                    currentWord.direction != otherWord.direction) {
                    
                    // Calculer les positions des cellules pour chaque mot
                    val currentCells = if (currentWord.direction == "horizontal") {
                        (0 until currentWord.length).map { 
                            Pair(currentWord.startRow, currentWord.startCol + it)
                        }
                    } else {
                        (0 until currentWord.length).map { 
                            Pair(currentWord.startRow + it, currentWord.startCol)
                        }
                    }
                    
                    val otherCells = if (otherWord.direction == "horizontal") {
                        (0 until otherWord.length).map { 
                            Pair(otherWord.startRow, otherWord.startCol + it)
                        }
                    } else {
                        (0 until otherWord.length).map { 
                            Pair(otherWord.startRow + it, otherWord.startCol)
                        }
                    }
                    
                    // Trouver les intersections
                    currentCells.forEachIndexed { index, cell ->
                        if (cell in otherCells) {
                            // Intersection trouvée! Position 1-indexed
                            crossings.add(Crossing(
                                position = index + 1,
                                crossingWordNumber = otherWord.number
                            ))
                        }
                    }
                }
            }
            
            currentWord.copy(crossings = crossings)
        }
    }
    
    private fun detectWords(grid: List<List<String>>): List<Word> {
        val words = mutableListOf<Word>()
        var wordNumber = 1
        
        val numRows = grid.size
        if (numRows == 0) return emptyList()
        val numCols = grid[0].size
        
        // Marquer les cellules qui commencent un mot
        val wordStarts = mutableMapOf<Pair<Int, Int>, Int>() // (row, col) -> word number
        
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                if (grid[row][col] == "white") {
                    var isWordStart = false
                    
                    // Vérifier si c'est le début d'un mot horizontal
                    val isHorizontalStart = (col == 0 || grid[row][col - 1] != "white") &&
                                           (col < numCols - 1 && grid[row][col + 1] == "white")
                    
                    // Vérifier si c'est le début d'un mot vertical
                    val isVerticalStart = (row == 0 || grid[row - 1][col] != "white") &&
                                         (row < numRows - 1 && grid[row + 1][col] == "white")
                    
                    if (isHorizontalStart || isVerticalStart) {
                        wordStarts[Pair(row, col)] = wordNumber
                        wordNumber++
                        isWordStart = true
                    }
                }
            }
        }
        
        // Maintenant, pour chaque mot détecté, calculer sa longueur
        for ((position, number) in wordStarts) {
            val (startRow, startCol) = position
            
            // Vérifier mot horizontal
            val isHorizontalStart = (startCol == 0 || grid[startRow][startCol - 1] != "white") &&
                                   (startCol < numCols - 1 && grid[startRow][startCol + 1] == "white")
            
            if (isHorizontalStart) {
                var length = 0
                var col = startCol
                while (col < numCols && grid[startRow][col] == "white") {
                    length++
                    col++
                }
                
                if (length >= 2) { // Minimum 2 lettres pour un mot
                    words.add(Word(
                        number = number,
                        direction = "horizontal",
                        startRow = startRow,
                        startCol = startCol,
                        length = length
                    ))
                }
            }
            
            // Vérifier mot vertical
            val isVerticalStart = (startRow == 0 || grid[startRow - 1][startCol] != "white") &&
                                 (startRow < numRows - 1 && grid[startRow + 1][startCol] == "white")
            
            if (isVerticalStart) {
                var length = 0
                var row = startRow
                while (row < numRows && grid[row][startCol] == "white") {
                    length++
                    row++
                }
                
                if (length >= 2) { // Minimum 2 lettres pour un mot
                    words.add(Word(
                        number = number,
                        direction = "vertical",
                        startRow = startRow,
                        startCol = startCol,
                        length = length
                    ))
                }
            }
        }
        
        println("🔤 Mots détectés: ${words.size}")
        words.forEach { word ->
            println("   #${word.number} ${word.direction}: position (${word.startRow},${word.startCol}), longueur ${word.length}")
        }
        
        // Trouver les croisements entre les mots
        val wordsWithCrossings = findCrossings(words.sortedBy { it.number })
        
        // Afficher les croisements
        wordsWithCrossings.forEach { word ->
            if (word.crossings.isNotEmpty()) {
                println("   #${word.number} croise: ${word.crossings.map { "mot ${it.crossingWordNumber} à la position ${it.position}" }.joinToString(", ")}")
            }
        }
        
        return wordsWithCrossings
    }
}

