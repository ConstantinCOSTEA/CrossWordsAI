package fr.miage.m1.solver

import java.text.Normalizer

object StringUtils {

    private val REGEX_ACCENTS = "\\p{M}".toRegex()
    private val REGEX_NON_ALPHA = "[^A-Z]".toRegex()

    fun removeAccents(str: String): String {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace(REGEX_ACCENTS, "")
    }

    /**
     * Nettoie une réponse : Accents -> UpperCase -> Garde seulement A-Z
     */
    fun cleanAnswer(str: String): String {
        return removeAccents(str)
            .uppercase()
            .replace(REGEX_NON_ALPHA, "")
    }
}