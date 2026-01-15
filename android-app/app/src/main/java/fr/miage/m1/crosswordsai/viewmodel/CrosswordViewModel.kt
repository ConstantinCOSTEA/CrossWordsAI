package fr.miage.m1.crosswordsai.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.miage.m1.crosswordsai.data.api.CrosswordApiService
import fr.miage.m1.crosswordsai.data.model.CrosswordData
import fr.miage.m1.crosswordsai.data.model.GridCell
import fr.miage.m1.crosswordsai.data.model.SseEvent
import fr.miage.m1.crosswordsai.data.repository.HistoryRepository
import fr.miage.m1.crosswordsai.data.repository.SavedCell
import fr.miage.m1.crosswordsai.data.repository.SavedGridData
import fr.miage.m1.crosswordsai.engine.CrosswordLayoutEngine
import fr.miage.m1.crosswordsai.util.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * États possibles du processus d'analyse
 */
sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Preprocessing : ProcessingState()
    data object Analyzing : ProcessingState()
    data object Solving : ProcessingState()
    data class Error(val message: String) : ProcessingState()
    data object Complete : ProcessingState()
}

class CrosswordViewModel(application: Application) : AndroidViewModel(application) {

    private val _gridState = MutableStateFlow<List<GridCell>>(emptyList())
    val gridState = _gridState.asStateFlow()

    private val _gridWidth = MutableStateFlow(0)
    val gridWidth = _gridWidth.asStateFlow()

    private val _gridHeight = MutableStateFlow(0)
    val gridHeight = _gridHeight.asStateFlow()

    private val _xAxisType = MutableStateFlow("")
    val xAxisType = _xAxisType.asStateFlow()

    private val _yAxisType = MutableStateFlow("")
    val yAxisType = _yAxisType.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState = _processingState.asStateFlow()

    private val _solvedCount = MutableStateFlow(0)
    val solvedCount = _solvedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount = _totalCount.asStateFlow()

    private val engine = CrosswordLayoutEngine()
    private val apiService = CrosswordApiService.getInstance()
    private val historyRepository = HistoryRepository(application)

    // Garde une référence aux données de la grille
    private var crosswordData: CrosswordData? = null

    // Callback pour naviguer vers la grille
    private var onGridReady: (() -> Unit)? = null

    /**
     * Définit le callback appelé quand la grille est prête
     */
    fun setOnGridReadyCallback(callback: () -> Unit) {
        onGridReady = callback
    }

    /**
     * Traite une image de grille de mots croisés
     * 1. Prétraitement de l'image
     * 2. Envoi au backend pour analyse
     * 3. Affichage de la grille
     * 4. Résolution en streaming
     */
    fun processImage(uri: Uri) {
        viewModelScope.launch {
            try {
                _processingState.value = ProcessingState.Preprocessing

                // 1. Prétraitement de l'image
                val processedFile = withContext(Dispatchers.IO) {
                    preprocessImage(uri)
                }

                _processingState.value = ProcessingState.Analyzing

                // 2. Envoyer au backend pour analyse
                val gridData = withContext(Dispatchers.IO) {
                    apiService.analyzeGrid(processedFile)
                }

                // 3. Initialiser la grille immédiatement
                crosswordData = gridData
                _gridWidth.value = gridData.width
                _gridHeight.value = gridData.height
                _xAxisType.value = gridData.x
                _yAxisType.value = gridData.y
                _totalCount.value = gridData.words.size

                val cells = engine.generateGrid(gridData)
                _gridState.value = cells

                println("✅ Grille analysée: ${gridData.width}x${gridData.height}, ${gridData.words.size} mots")

                // Naviguer vers la grille
                onGridReady?.invoke()

                _processingState.value = ProcessingState.Solving

                // 4. Lancer la résolution en streaming
                solveWithStreaming(gridData)

                // Nettoyer le fichier temporaire
                processedFile.delete()

            } catch (e: Exception) {
                e.printStackTrace()
                _processingState.value = ProcessingState.Error("Erreur: ${e.message}")
            }
        }
    }

    /**
     * Prétraite l'image et la sauvegarde dans un fichier temporaire
     */
    private fun preprocessImage(uri: Uri): File {
        val context = getApplication<Application>()
        
        // Charger le bitmap depuis l'URI
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Impossible de lire l'image")
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Prétraiter l'image
        val processedBitmap = ImagePreprocessor.preprocess(originalBitmap)
        originalBitmap.recycle()

        // Sauvegarder dans un fichier temporaire
        val outputFile = File(context.cacheDir, "processed_grid_${System.currentTimeMillis()}.png")
        FileOutputStream(outputFile).use { out ->
            processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        processedBitmap.recycle()

        println("✅ Image prétraitée: ${outputFile.absolutePath}")
        return outputFile
    }

    /**
     * Lance la résolution en streaming SSE
     */
    private suspend fun solveWithStreaming(gridData: CrosswordData) {
        try {
            apiService.solveGridStream(gridData).collect { event ->
                when (event) {
                    is SseEvent.Connected -> {
                        println("🔗 SSE Connecté")
                    }
                    is SseEvent.Round -> {
                        println("📦 Round ${event.event.round}: ${event.event.words.size} mots")
                        event.event.words.forEach { word ->
                            fillWord(word.number, word.order, word.direction, word.answer)
                        }
                        _solvedCount.value = event.event.solved
                    }
                    is SseEvent.Complete -> {
                        println("✅ Résolution terminée: ${event.event.solved}/${event.event.total}")
                        event.event.words.forEach { word ->
                            fillWord(word.number, word.order, word.direction, word.answer)
                        }
                        _solvedCount.value = event.event.solved
                        _processingState.value = ProcessingState.Complete
                        
                        // Sauvegarder automatiquement dans l'historique
                        saveToHistory()
                    }
                    is SseEvent.Error -> {
                        println("❌ Erreur SSE: ${event.message}")
                        _processingState.value = ProcessingState.Error(event.message)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _processingState.value = ProcessingState.Error("Erreur streaming: ${e.message}")
        }
    }

    /**
     * Sauvegarde la grille actuelle dans l'historique
     */
    private fun saveToHistory() {
        val cells = _gridState.value
        if (cells.isEmpty()) return

        val savedCells = cells.map { cell ->
            SavedCell(
                x = cell.x,
                y = cell.y,
                char = cell.char,
                number = cell.number,
                isEmpty = cell.isEmpty
            )
        }

        val savedData = SavedGridData(
            width = _gridWidth.value,
            height = _gridHeight.value,
            cells = savedCells,
            xAxisType = _xAxisType.value,
            yAxisType = _yAxisType.value,
            solvedCount = _solvedCount.value,
            totalCount = _totalCount.value
        )

        historyRepository.saveGrid(savedData)
        println("✅ Grille sauvegardée dans l'historique")
    }

    /**
     * Charge une grille depuis l'historique
     */
    fun loadFromHistory(savedData: SavedGridData) {
        _gridWidth.value = savedData.width
        _gridHeight.value = savedData.height
        _xAxisType.value = savedData.xAxisType
        _yAxisType.value = savedData.yAxisType
        _solvedCount.value = savedData.solvedCount
        _totalCount.value = savedData.totalCount

        val cells = savedData.cells.map { saved ->
            GridCell(
                x = saved.x,
                y = saved.y,
                char = saved.char,
                number = saved.number,
                isEmpty = saved.isEmpty
            )
        }

        _gridState.value = cells
        _processingState.value = ProcessingState.Complete
        
        println("✅ Grille chargée depuis l'historique: ${savedData.width}x${savedData.height}")
    }

    /**
     * Remplit un mot dans la grille avec la réponse fournie
     */
    fun fillWord(number: Int, order: Int, direction: String, answer: String) {
        val data = crosswordData ?: run {
            println("⚠️ Grille non chargée")
            return
        }

        // Récupérer les mots auto-détectés depuis le moteur
        val detectedWords = engine.getDetectedWords(data)
        
        // Trouve le mot correspondant
        val word = detectedWords.find {
            it.number == number && it.order == order && it.direction == direction
        } ?: run {
            println("⚠️ Mot $number (ordre:$order) $direction introuvable")
            return
        }

        // Vérifie que la longueur correspond
        if (answer.length != word.size) {
            println("⚠️ Erreur: '${answer}' fait ${answer.length} lettres, attendu ${word.size}")
            return
        }

        // Récupère la position du mot depuis l'engine
        val wordPosition = engine.getWordPosition(word.idKey) ?: run {
            println("⚠️ Position du mot introuvable")
            return
        }

        val (startX, startY) = wordPosition

        // Calcule les positions de chaque lettre
        val dx = if (direction == "horizontal") 1 else 0
        val dy = if (direction == "vertical") 1 else 0

        // Met à jour la grille
        val updatedCells = _gridState.value.map { cell ->
            for (i in answer.indices) {
                val letterX = startX + (i * dx)
                val letterY = startY + (i * dy)

                if (cell.x == letterX && cell.y == letterY) {
                    return@map cell.copy(char = answer[i].uppercaseChar())
                }
            }
            cell
        }

        _gridState.value = updatedCells
        println("✅ Mot $number (ordre:$order) $direction rempli: $answer")
    }

    /**
     * Réinitialise l'état pour une nouvelle image
     */
    fun reset() {
        _gridState.value = emptyList()
        _gridWidth.value = 0
        _gridHeight.value = 0
        _processingState.value = ProcessingState.Idle
        _solvedCount.value = 0
        _totalCount.value = 0
        crosswordData = null
    }
}
