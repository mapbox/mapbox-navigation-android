package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import java.net.URI

/**
 * Defines options for routing tiles of a specific domain endpoint and storage configuration.
 *
 * @param tilesBaseUri scheme and host, for example `"https://api.mapbox.com"`.
 * If empty, the navigator works in the fallback mode for this domain all the time
 * (route line following; no full map-matching; no map-matching in free drive).
 * @param tilesDataset string built out of `<account>[.<graph>]` variables.
 * Account can be `mapbox` for default datasets or your username for other.
 * Graph can be left blank if you don't target any custom datasets.
 * @param tilesProfile profile of the dataset.
 * One of (driving|driving-traffic|walking|cycling|truck).
 * @param tilesVersion version of tiles, chosen automatically if empty.
 * @param minDaysBetweenServerAndLocalTilesVersion is the minimum time in days between local version of tiles
 * and latest on the server to consider using the latest version of routing tiles of this specific domain from the server.
 * **As updating tiles frequently consumes considerably energy and bandwidth**.
 * Note that this only works if [tilesVersion] is empty.
 */
@ExperimentalMapboxNavigationAPI
class DomainTilesOptions private constructor(
    val tilesBaseUri: URI,
    val tilesDataset: String,
    val tilesProfile: String,
    val tilesVersion: String,
    val minDaysBetweenServerAndLocalTilesVersion: Int,
) {

    /**
     * @return the builder that created the [DomainTilesOptions]
     */
    fun toBuilder(): Builder = Builder(
        tilesBaseUri,
        tilesDataset,
        tilesProfile,
        tilesVersion,
        minDaysBetweenServerAndLocalTilesVersion,
    )

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DomainTilesOptions

        if (tilesBaseUri != other.tilesBaseUri) return false
        if (tilesDataset != other.tilesDataset) return false
        if (tilesProfile != other.tilesProfile) return false
        if (tilesVersion != other.tilesVersion) return false
        if (minDaysBetweenServerAndLocalTilesVersion !=
            other.minDaysBetweenServerAndLocalTilesVersion
        ) {
            return false
        }

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = tilesBaseUri.hashCode()
        result = 31 * result + tilesDataset.hashCode()
        result = 31 * result + tilesProfile.hashCode()
        result = 31 * result + tilesVersion.hashCode()
        result = 31 * result + minDaysBetweenServerAndLocalTilesVersion
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "DomainTilesOptions(" +
            "tilesBaseUri=$tilesBaseUri, " +
            "tilesDataset='$tilesDataset', " +
            "tilesProfile='$tilesProfile', " +
            "tilesVersion='$tilesVersion', " +
            "minDaysBetweenServerAndLocalTilesVersion=$minDaysBetweenServerAndLocalTilesVersion" +
            ")"
    }

    @ExperimentalMapboxNavigationAPI
    companion object {

        /**
         * Returns a Builder representing default configuration for HD tiles.
         */
        fun defaultHdTilesOptionsBuilder(): Builder = Builder(
            tilesBaseUri = URI("https://api-3dln-tiles-stagyprod.tilestream.net"),
            tilesDataset = "mapbox",
            tilesProfile = "",
            tilesVersion = "",
            minDaysBetweenServerAndLocalTilesVersion = 7,
        )

        /**
         * Returns [DomainTilesOptions] representing default configuration for HD tiles.
         */
        fun defaultHdTilesOptions(): DomainTilesOptions = defaultHdTilesOptionsBuilder().build()
    }

    /**
     * Builder for [DomainTilesOptions].
     */
    @ExperimentalMapboxNavigationAPI
    class Builder(
        private var tilesBaseUri: URI,
        private var tilesDataset: String,
        private var tilesProfile: String,
        private var tilesVersion: String,
        private var minDaysBetweenServerAndLocalTilesVersion: Int,
    ) {

        /**
         * Override the base endpoint for the routing tiles of this specific domain.
         *
         * Expects scheme and host, for example `"https://api.mapbox.com"`.
         * If empty, the navigator works in the fallback mode all the time
         * (route line following; no full map-matching; no map-matching in free drive).
         */
        fun tilesBaseUri(tilesBaseUri: URI): Builder =
            apply {
                val validUri = tilesBaseUri.isAbsolute &&
                    tilesBaseUri.host.isNotEmpty() &&
                    tilesBaseUri.fragment == null &&
                    tilesBaseUri.path.let { it.isNullOrEmpty() || it.matches("/*".toRegex()) } &&
                    tilesBaseUri.query == null
                check(validUri) {
                    throw IllegalArgumentException(
                        "The base URI should only contain the scheme and host.",
                    )
                }
                this.tilesBaseUri = tilesBaseUri
            }

        /**
         * String built out of `<account>[.<graph>]` variables.
         * Account can be `mapbox` for default datasets or your username for other.
         * Graph can be left blank if you don't have any no custom datasets.
         * Defaults to `mapbox`.
         */
        fun tilesDataset(tilesDataset: String): Builder =
            apply { this.tilesDataset = tilesDataset }

        /**
         * Profile of the dataset. One of (driving|driving-traffic|walking|cycling|truck).
         * Defaults to `driving-traffic`.
         */
        fun tilesProfile(tilesProfile: String): Builder =
            apply { this.tilesProfile = tilesProfile }

        /**
         * Override the default routing tiles version for this specific domain.
         * If empty, the version will be chosen automatically.
         */
        fun tilesVersion(version: String): Builder =
            apply { this.tilesVersion = version }

        /**
         * Override minimum time in days between local version of tiles of this specific domain
         * and latest on the server to consider using the latest version of routing tiles from the server.
         * This only works if tiles version is empty.
         *
         * Note that **updating tiles frequently consumes considerably energy and bandwidth**.
         *
         * @param minDaysBetweenServerAndLocalTilesVersion must be greater than or equal to 0
         * @see [tilesVersion]
         */
        fun minDaysBetweenServerAndLocalTilesVersion(
            minDaysBetweenServerAndLocalTilesVersion: Int,
        ): Builder =
            apply {
                check(minDaysBetweenServerAndLocalTilesVersion >= 0) {
                    "minDaysBetweenServerAndLocalTilesVersion must be greater than or equal to 0"
                }
                this.minDaysBetweenServerAndLocalTilesVersion =
                    minDaysBetweenServerAndLocalTilesVersion
            }

        /**
         * Build the [DomainTilesOptions].
         */
        fun build(): DomainTilesOptions {
            return DomainTilesOptions(
                tilesBaseUri,
                tilesDataset,
                tilesProfile,
                tilesVersion,
                minDaysBetweenServerAndLocalTilesVersion,
            )
        }
    }
}
