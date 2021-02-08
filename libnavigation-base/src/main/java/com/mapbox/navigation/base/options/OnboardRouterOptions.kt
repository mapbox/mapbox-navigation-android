package com.mapbox.navigation.base.options

import java.net.URI

/**
 * Defines options for on-board router. These options enable a feature also known as Free Drive.
 * This allows the navigator to map-match your location onto the road network without a route.
 *
 * @param tilesUri tiles endpoint
 * @param tilesVersion version of tiles. If not specified (empty), latest version is used
 * @param filePath used for storing road network tiles
 * @param keepOlderTilesVersions true if older version tiles must be kept, false if not. Note
 * that this only works if [tilesVersion] is empty
 * @param minDaysBetweenServerAndLocalTilesVersion is the minimum time in days between local version of tiles
 * and latest on the server to consider using the latest version of routing tiles from the server.
 * **As updating tiles frequently consumes considerably energy and bandwidth**.
 * Note that this only works if [tilesVersion] is empty.
 */
class OnboardRouterOptions private constructor(
    val tilesUri: URI,
    val tilesVersion: String,
    val filePath: String?,
    val keepOlderTilesVersions: Boolean,
    val minDaysBetweenServerAndLocalTilesVersion: Int
) {
    /**
     * @return the builder that created the [OnboardRouterOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        tilesUri(tilesUri)
        tilesVersion(tilesVersion)
        filePath(filePath)
        keepOlderTilesVersions(keepOlderTilesVersions)
        minDaysBetweenServerAndLocalTilesVersion(minDaysBetweenServerAndLocalTilesVersion)
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
        if (keepOlderTilesVersions != other.keepOlderTilesVersions) return false
        if (minDaysBetweenServerAndLocalTilesVersion
            != other.minDaysBetweenServerAndLocalTilesVersion
        ) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = tilesUri.hashCode()
        result = 31 * result + tilesVersion.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + keepOlderTilesVersions.hashCode()
        result = 31 * result + minDaysBetweenServerAndLocalTilesVersion.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "OnboardRouterOptions(" +
            "tilesUri=$tilesUri, " +
            "tilesVersion='$tilesVersion', " +
            "filePath=$filePath, " +
            "keepOlderTilesVersions=$keepOlderTilesVersions, " +
            "minDaysBetweenServerAndLocalTilesVersion=$minDaysBetweenServerAndLocalTilesVersion" +
            ")"
    }

    /**
     * Builder for [OnboardRouterOptions]. You must choose a [filePath]
     * for this to be built successfully.
     */
    class Builder {
        private var tilesUri: URI = URI("https://api.mapbox.com")
        private var tilesVersion: String = ""
        private var filePath: String? = null
        private var keepOlderTilesVersions: Boolean = false
        private var minDaysBetweenServerAndLocalTilesVersion: Int = 56 // 8 weeks

        /**
         * Override the routing tiles endpoint with a [URI]
         */
        fun tilesUri(tilesUri: URI): Builder =
            apply { this.tilesUri = tilesUri }

        /**
         * Override the routing tiles version.
         * If not specified (empty), latest tiles version is used.
         *
         * Default value is **empty**.
         */
        fun tilesVersion(version: String): Builder =
            apply { this.tilesVersion = version }

        /**
         * Creates a custom file path to store the road network tiles.
         */
        fun filePath(filePath: String?): Builder =
            apply { this.filePath = filePath }

        /**
         * Override flag to keep/remove older tiles versions. Note that this only works if tiles
         * version is empty.
         *
         * Default value is **false**.
         *
         * @see [tilesVersion]
         */
        fun keepOlderTilesVersions(keepOlderTilesVersions: Boolean): Builder =
            apply { this.keepOlderTilesVersions = keepOlderTilesVersions }

        /**
         * Override minimum time in days between local version of tiles and latest on the server
         * to consider using the latest version of routing tiles from the server. This only works
         * if tiles version is empty.
         *
         * Note that **updating tiles frequently consumes considerably energy and bandwidth**.
         *
         * Default value is **56** (8 weeks).
         *
         * @param minDaysBetweenServerAndLocalTilesVersion must be greater than or equal to 0
         * @see [tilesVersion]
         */
        fun minDaysBetweenServerAndLocalTilesVersion(
            minDaysBetweenServerAndLocalTilesVersion: Int
        ): Builder =
            apply {
                check(minDaysBetweenServerAndLocalTilesVersion >= 0) {
                    "minDaysBetweenServerAndLocalTilesVersion must be greater than or equal to 0"
                }
                this.minDaysBetweenServerAndLocalTilesVersion =
                    minDaysBetweenServerAndLocalTilesVersion
            }

        /**
         * Build the [OnboardRouterOptions]
         */
        fun build(): OnboardRouterOptions {
            return OnboardRouterOptions(
                tilesUri = tilesUri,
                tilesVersion = tilesVersion,
                filePath = filePath,
                keepOlderTilesVersions = keepOlderTilesVersions,
                minDaysBetweenServerAndLocalTilesVersion = minDaysBetweenServerAndLocalTilesVersion
            )
        }
    }
}
