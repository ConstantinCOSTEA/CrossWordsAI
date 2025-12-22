package fr.miage.m1.schemas

import kotlinx.serialization.Serializable

/**
 * Information sur un croisement entre deux mots
 */
@Serializable
data class CrossingInfo(
    val position: Int,      // Position dans le mot actuel (1-indexed)
    val crossingWordNumber: Int  // Numéro du mot qui croise
)

/**
 * Information sur un mot détecté dans la grille
 */
@Serializable
data class WordInfo(
    val number: Int,
    val size: Int,  // Taille du mot (nombre de lettres)
    val direction: String, // "vertical" ou "horizontal"
    val crossings: List<CrossingInfo>  // Liste des croisements
)

/**
 * Question pour un mot de la grille
 */
@Serializable
data class Question(
    val number: Int,           // Numéro du mot
    val direction: String,     // "horizontal" ou "vertical"
    val text: String          // Texte de la question
)

/**
 * Réponse de l'API d'analyse de grille
 */
@Serializable
data class GridResponse(
    val words: List<WordInfo>,
    val annotatedImageUrl: String? = null
)

/**
 * Réponse de l'API d'extraction de questions
 */
@Serializable
data class QuestionsResponse(
    val questions: List<Question>? = null,  // Format structuré (optionnel)
    val rawText: String? = null              // Texte brut extrait par OCR
)

