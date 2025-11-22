package fr.univcotedazur.crosswordsai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CrosswordData(
    val words: List<WordDefinition>
)

@Serializable
data class WordDefinition(
    val number: Int,
    val size: Int,
    val direction: String,
    val crossings: List<Crossing> = emptyList()
) {
    val idKey: String
        get() = "${number}_$direction"
}

@Serializable
data class Crossing(
    val position: Int,
    val crossingWordNumber: Int
)

data class GridCell(
    val x: Int,
    val y: Int,
    val number: Int? = null,
    val char: Char? = null,
    val isEmpty: Boolean = false
)