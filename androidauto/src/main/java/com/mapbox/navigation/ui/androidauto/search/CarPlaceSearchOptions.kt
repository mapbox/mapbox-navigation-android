package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.base.options.NavigationOptions

/**
 * Options for the Car Mapbox Place Search.
 *
 * @param accessToken place search access token. If null the navigation access token is used.
 */
class CarPlaceSearchOptions private constructor(
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
         * Override the Mapbox Search access token. If null, the access token provided to
         * [NavigationOptions.accessToken] will be used.
         */
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
