package fr.univcotedazur.crosswordsai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WordAnswersResponse(
    val words: List<WordAnswer>
)

@Serializable
data class WordAnswer(
    val number: Int,
    val direction: String,
    val answer: String
)