package com.mapbox.navigation.base.options

import java.net.URI

/**
 * Defines options for on-board router. These options enable a feature also known as Free Drive.
 * This allows the navigator to map-match your location onto the road network without a route.
 *
 * @param tilesUri tiles endpoint
 * @param tilesVersion version of tiles
 * @param filePath used for storing road network tiles
 * @param builder used for updating options
 */
data class OnboardRouterOptions(
    val tilesUri: URI,
    val tilesVersion: String,
    val filePath: String?,
    val builder: Builder
) {
    /**
     * @return the builder that created the [OnboardRouterOptions]
     */
    fun toBuilder() = builder

    /**
     * Builder for [OnboardRouterOptions]. You must choose a [filePath]
     * for this to be built successfully.
     */
    class Builder {
        private var tilesUri: URI = URI("https://api.mapbox.com")
        private var tilesVersion: String = "2020_02_02-03_00_00"
        private var filePath: String? = null

        /**
         * Override the routing tiles endpoint with a [String]
         */
        fun tilesUri(tilesUri: String) =
            apply { this.tilesUri = URI(tilesUri) }

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
        fun filePath(filePath: String) =
            apply { this.filePath = filePath }

        /**
         * Build the [OnboardRouterOptions]
         */
        fun build(): OnboardRouterOptions {
            return OnboardRouterOptions(
                tilesUri = tilesUri,
                tilesVersion = tilesVersion,
                filePath = filePath,
                builder = this
            )
        }
    }
}
