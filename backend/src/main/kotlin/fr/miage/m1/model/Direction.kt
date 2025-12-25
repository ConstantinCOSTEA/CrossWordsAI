package fr.miage.m1.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Direction d'un mot dans la grille
 */
@Serializable
enum class Direction {
    @SerialName("horizontal") HORIZONTAL,
    @SerialName("vertical") VERTICAL
}
