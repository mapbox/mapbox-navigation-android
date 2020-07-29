package com.mapbox.navigation.base.options

import java.net.URI

/**
 * Defines options for on-board router. These options enable a feature also known as Free Drive.
 * This allows the navigator to map-match your location onto the road network without a route.
 *
 * @param tilesUri tiles endpoint
 * @param tilesVersion version of tiles
 * @param filePath used for storing road network tiles
 */
class OnboardRouterOptions private constructor(
    val tilesUri: URI,
    val tilesVersion: String,
    val filePath: String?
) {
    /**
     * @return the builder that created the [OnboardRouterOptions]
     */
    fun toBuilder() = Builder().apply {
        tilesUri(tilesUri)
        tilesVersion(tilesVersion)
        filePath(filePath)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OnboardRouterOptions

        if (tilesUri != other.tilesUri) return false
        if (tilesVersion != other.tilesVersion) return false
        if (filePath != other.filePath) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = tilesUri.hashCode()
        result = 31 * result + tilesVersion.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OnboardRouterOptions(tilesUri=$tilesUri, tilesVersion='$tilesVersion', filePath=$filePath)"
    }

    /**
     * Builder for [OnboardRouterOptions]. You must choose a [filePath]
     * for this to be built successfully.
     */
    class Builder {
        private var tilesUri: URI = URI("https://api.mapbox.com")
        private var tilesVersion: String = "2020_02_02-03_00_00"
        private var filePath: String? = null

        /**
         * Override the routing tiles endpoint with a [URI]
         */
        fun tilesUri(tilesUri: URI) =
            apply { this.tilesUri = tilesUri }

        /**
         * Override the routing tiles version.
         */
        fun tilesVersion(version: String) =
            apply { this.tilesVersion = version }

        /**
         * Creates a custom file path to store the road network tiles.
         */
        fun filePath(filePath: String?) =
            apply { this.filePath = filePath }

        /**
         * Build the [OnboardRouterOptions]
         */
        fun build(): OnboardRouterOptions {
            return OnboardRouterOptions(
                tilesUri = tilesUri,
                tilesVersion = tilesVersion,
                filePath = filePath
            )
        }
    }
}
