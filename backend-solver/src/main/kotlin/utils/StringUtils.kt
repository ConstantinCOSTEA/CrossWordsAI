package fr.univcotedazur.utils

import java.text.Normalizer

/**
 * Utilitaires pour manipuler les chaînes de caractères
 */
object StringUtils {
    
    /**
     * Enlève tous les accents d'une chaîne de caractères
     * 
     * Exemples:
     * - "ÉLÉPHANT" -> "ELEPHANT"
     * - "CHÂTEAU" -> "CHATEAU"
     * - "NOËL" -> "NOEL"
     */
    fun removeAccents(str: String): String {
        // Normaliser en décomposant les caractères accentués
        val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
        // Supprimer les marques diacritiques (accents)
        return normalized.replace("\\p{M}".toRegex(), "")
    }
}
