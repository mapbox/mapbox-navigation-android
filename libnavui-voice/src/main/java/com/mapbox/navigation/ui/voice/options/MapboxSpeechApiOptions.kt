package com.mapbox.navigation.ui.voice.options

/**
 * MapboxSpeechApiOptions.
 *
 * @param baseUri base URL
 * @param gender voice gender
 */
class MapboxSpeechApiOptions private constructor(
    val baseUri: String,
    @VoiceGender.Type val gender: String?
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        baseUri(baseUri)
        gender(gender)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxSpeechApiOptions

        if (baseUri != other.baseUri) return false
        if (gender != other.gender) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = baseUri.hashCode()
        result = 31 * result + (gender?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxSpeechApiOptions(" +
            "baseUri=$baseUri, " +
            "gender=$gender" +
            ")"
    }

    /**
     * Build a new [MapboxSpeechApiOptions]
     */
    class Builder {

        private var baseUri: String = "https://api.mapbox.com"
        private var gender: String? = null

        /**
         * Specifies the base URL
         * Defaults to `"https://api.mapbox.com"`
         */
        fun baseUri(baseUri: String): Builder =
            apply { this.baseUri = baseUri }

        /**
         * Specifies gender of the voice.
         * Optional parameter. Might not work with all the languages.
         * Defaults to [VoiceGender.FEMALE].
         */
        fun gender(@VoiceGender.Type gender: String?): Builder =
            apply { this.gender = gender }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): MapboxSpeechApiOptions {
            return MapboxSpeechApiOptions(
                baseUri = baseUri,
                gender = gender,
            )
        }
    }
}
