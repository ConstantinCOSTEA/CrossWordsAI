package fr.univcotedazur.crosswordsai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.univcotedazur.crosswordsai.engine.CrosswordLayoutEngine
import fr.univcotedazur.crosswordsai.data.model.CrosswordData
import fr.univcotedazur.crosswordsai.data.model.GridCell
import fr.univcotedazur.crosswordsai.data.model.WordAnswersResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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

    private val engine = CrosswordLayoutEngine()

    // Instance Json réutilisable
    private val json = Json { ignoreUnknownKeys = true }

    // Garde une référence aux données de la grille
    private var crosswordData: CrosswordData? = null

    init {
        // Charge la grille
        loadGrid()

        //TODO: A remplacer par un appel API
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            loadAnswers()
        }
    }

    /**
     * Charge la structure de la grille depuis le premier JSON
     */
    private fun loadGrid() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets
                    .open("crossword_grid_test.json")
                    .bufferedReader()
                    .use { it.readText() }

                val data = json.decodeFromString<CrosswordData>(jsonString)
                crosswordData = data

                // Définir les métadonnées de la grille pour l'UI
                _gridWidth.value = data.width
                _gridHeight.value = data.height
                _xAxisType.value = data.x
                _yAxisType.value = data.y

                val cells = engine.generateGrid(data)
                _gridState.value = cells

                println("✅ Grille chargée: ${cells.size} cases")

            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Erreur chargement grille: ${e.message}")
            }
        }
    }

    /**
     * Charge les réponses depuis le deuxième JSON
     */
    private fun loadAnswers() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets
                    .open("crossword_answers_test.json")
                    .bufferedReader()
                    .use { it.readText() }

                fillWordsFromJson(jsonString)
                println("✅ Réponses chargées")

            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Erreur chargement réponses: ${e.message}")
            }
        }
    }

    /**
     * Remplit un mot dans la grille avec la réponse fournie
     * @param number Le numéro du mot
     * @param order L'ordre du mot sur sa ligne/colonne (1er, 2ème, 3ème...)
     * @param direction "horizontal" ou "vertical"
     * @param answer La réponse (ex: "AGNEAU")
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
     * Remplit plusieurs mots à la fois depuis un JSON de réponses
     */
    fun fillWordsFromJson(jsonResponse: String) {
        viewModelScope.launch {
            try {
                val response = json.decodeFromString<WordAnswersResponse>(jsonResponse)

                response.words.forEach { wordAnswer ->
                    fillWord(wordAnswer.number, wordAnswer.order, wordAnswer.direction, wordAnswer.answer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Erreur parsing réponses: ${e.message}")
            }
        }
    }

    /**
     * Méthode publique pour charger les réponses depuis un JSON string
     * (à utiliser quand on reçoit le JSON depuis le backend)
     */
    fun loadAnswersFromBackend(jsonResponse: String) {
        fillWordsFromJson(jsonResponse)
    }
}