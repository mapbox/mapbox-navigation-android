package com.mapbox.navigation.base.options

import java.net.URI

/**
 * Defines options for on-board router. These options enable a feature also known as Free Drive.
 * This allows the navigator to map-match your location onto the road network without a route.
 *
 * @param tilesUri tiles endpoint
 * @param tilesDataset string built out of `<account>[.<graph>]` variables.
 * Account can be `mapbox` for default datasets or your username for other.
 * Graph can be left blank if you don't target any custom datasets.
 * @param tilesProfile profile of the dataset.
 * One of (driving|driving-traffic|walking|cycling|truck).
 * @param tilesVersion version of tiles
 * @param filePath used for storing road network tiles
 */
class OnboardRouterOptions private constructor(
    val tilesUri: URI,
    val tilesDataset: String,
    val tilesProfile: String,
    val tilesVersion: String,
    val filePath: String?
) {
    /**
     * @return the builder that created the [OnboardRouterOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        tilesUri(tilesUri)
        tilesDataset(tilesDataset)
        tilesProfile(tilesProfile)
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
        if (tilesDataset != other.tilesDataset) return false
        if (tilesProfile != other.tilesProfile) return false
        if (tilesVersion != other.tilesVersion) return false
        if (filePath != other.filePath) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = tilesUri.hashCode()
        result = 31 * result + tilesDataset.hashCode()
        result = 31 * result + tilesProfile.hashCode()
        result = 31 * result + tilesVersion.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OnboardRouterOptions(" +
            "tilesUri=$tilesUri, " +
            "tilesDataset='$tilesDataset', " +
            "tilesProfile='$tilesProfile', " +
            "tilesVersion='$tilesVersion', " +
            "filePath=$filePath" +
            ")"
    }

    /**
     * Builder for [OnboardRouterOptions]. You must choose a [filePath]
     * for this to be built successfully.
     */
    class Builder {

        private var tilesUri: URI = URI("https://api.mapbox.com")
        private var tilesDataset: String = "mapbox"
        private var tilesProfile: String = "driving-traffic"
        private var tilesVersion: String = "2021_01_30-03_00_00"
        private var filePath: String? = null

        /**
         * Override the routing tiles endpoint with a [URI].
         */
        fun tilesUri(tilesUri: URI): Builder =
            apply { this.tilesUri = tilesUri }

        /**
         * String built out of `<account>[.<graph>]` variables.
         *
         * Account can be `mapbox` for default datasets or your username for other.
         *
         * Graph can be left blank if you don't have any no custom datasets.
         *
         * Defaults to `mapbox`.
         */
        fun tilesDataset(tilesDataset: String): Builder =
            apply { this.tilesDataset = tilesDataset }

        /**
         * Profile of the dataset. One of (driving|driving-traffic|walking|cycling|truck).
         *
         * Defaults to `driving-traffic`.
         */
        fun tilesProfile(tilesProfile: String): Builder =
            apply { this.tilesProfile = tilesProfile }

        /**
         * Override the default routing tiles version.
         *
         * You can find available version by calling:
         * `<host>/route-tiles/v2/<account>[.<graph>]/<profile>/versions?access_token=<your_token>`
         *
         * where profile is one of (driving|driving-traffic|walking|cycling|truck).
         */
        fun tilesVersion(version: String): Builder =
            apply { this.tilesVersion = version }

        /**
         * Creates a custom file path to store the road network tiles.
         */
        fun filePath(filePath: String?): Builder =
            apply { this.filePath = filePath }

        /**
         * Build the [OnboardRouterOptions]
         */
        fun build(): OnboardRouterOptions {
            return OnboardRouterOptions(
                tilesUri = tilesUri,
                tilesDataset = tilesDataset,
                tilesProfile = tilesProfile,
                tilesVersion = tilesVersion,
                filePath = filePath
            )
        }
    }
}
