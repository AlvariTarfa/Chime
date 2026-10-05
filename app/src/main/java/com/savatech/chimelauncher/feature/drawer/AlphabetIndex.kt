package com.savatech.chimelauncher.feature.drawer

import java.text.Normalizer
import java.util.Locale

object AlphabetIndex {
    val entries: List<String> = ('A'..'Z').map(Char::toString) + "#"

    fun bucketFor(label: String): String {
        val firstCharacter = label.firstOrNull() ?: return "#"
        val normalized = Normalizer.normalize(firstCharacter.toString(), Normalizer.Form.NFD)
        val letter = normalized.firstOrNull {
            val type = Character.getType(it)
            type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
        }
            ?.uppercase(Locale.ROOT)
            ?.firstOrNull()
        return if (letter in 'A'..'Z') letter.toString() else "#"
    }
}
