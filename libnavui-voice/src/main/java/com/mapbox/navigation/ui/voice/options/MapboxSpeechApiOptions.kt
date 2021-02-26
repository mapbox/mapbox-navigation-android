package com.mapbox.navigation.ui.voice.options

/**
 * MapboxSpeechApiOptions.
 *
 * @param baseUri base URL
 */
class MapboxSpeechApiOptions private constructor(
    val baseUri: String
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        baseUri(baseUri)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxSpeechApiOptions

        if (baseUri != other.baseUri) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return baseUri.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxSpeechApiOptions(" +
            "baseUri=$baseUri" +
            ")"
    }

    /**
     * Build a new [MapboxSpeechApiOptions]
     */
    class Builder {

        private var baseUri: String = "https://api.mapbox.com"

        /**
         * Specifies the base URL
         * Defaults to `"https://api.mapbox.com"`
         */
        fun baseUri(baseUri: String): Builder =
            apply { this.baseUri = baseUri }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): MapboxSpeechApiOptions {
            return MapboxSpeechApiOptions(
                baseUri = baseUri
            )
        }
    }
}
