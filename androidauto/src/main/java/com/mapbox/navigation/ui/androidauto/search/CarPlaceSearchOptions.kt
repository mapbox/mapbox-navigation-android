package com.mapbox.navigation.ui.androidauto.search

/**
 * Options for the Car Mapbox Place Search.
 *
 * @param accessToken place search access token.
 * No longer in use. The search component now uses the default MapboxOptions.accessToken
 */
class CarPlaceSearchOptions private constructor(
    @Deprecated(
        "No longer in use. The search component now uses the default MapboxOptions.accessToken",
    )
    val accessToken: String?,
) {
    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        accessToken(accessToken)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CarPlaceSearchOptions

        if (accessToken != other.accessToken) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return accessToken?.hashCode() ?: 0
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "CarPlaceSearchOptions(accessToken=$accessToken)"
    }

    /**
     * Build a new [CarPlaceSearchOptions]
     */
    class Builder {
        private var accessToken: String? = null

        /**
         * Override the Mapbox Search access token.
         * No longer in use. The search component now uses the default MapboxOptions.accessToken
         */
        @Deprecated(
            "No longer in use. The search component now uses the default MapboxOptions.accessToken",
        )
        fun accessToken(accessToken: String?) = apply {
            this.accessToken = accessToken
        }

        /**
         * Build the [CarPlaceSearchOptions]
         */
        fun build(): CarPlaceSearchOptions {
            return CarPlaceSearchOptions(
                accessToken = accessToken,
            )
        }
    }
}
