package com.mapbox.navigation.mapgpt.core.language

import java.util.Locale

/**
 * Language object that can be used per platform.
 *
 * @param languageTag IETF language tag (based on ISO 639), for example "en-US".
 */
class Language(
    val locale: Locale,
) {

    constructor(languageTag: String) : this(Locale.forLanguageTag(languageTag))

    val languageTag: String = locale.toLanguageTag()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Language) return false

        return this.locale.language == other.locale.language
    }

    override fun hashCode(): Int {
        return locale.language.hashCode()
    }

    override fun toString(): String {
        return "Language(locale=$locale)"
    }
}

/**
 * Returns the current language of the hosting device.
 */
fun deviceLanguage(): Language {
    return Language(Locale.getDefault())
}
