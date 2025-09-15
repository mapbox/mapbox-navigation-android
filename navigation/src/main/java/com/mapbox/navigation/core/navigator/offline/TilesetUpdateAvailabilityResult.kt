package com.mapbox.navigation.core.navigator.offline

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Result of checking tileset update availability for a specific region.
 * This sealed class represents the outcome of checking whether a tileset update
 * is available for a given region. It can indicate either that no updates are
 * needed or that updates are available with additional metadata.
 * @see TilesetVersionManager.getAvailableUpdate for usage
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class TilesetUpdateAvailabilityResult internal constructor() {

    /**
     * Indicates that no tileset updates are available for the region.
     * This result is returned when the currently downloaded tileset version
     * is up to date with the latest available version.
     */
    object NoUpdates : TilesetUpdateAvailabilityResult()

    /**
     * Indicates that tileset updates are available for the region.
     * @param regionId ID of the region for which update is available/required
     * @param isAsap True if the current version is not recommended to be used and should be updated ASAP
     * @param currentVersion Current version of the downloaded tileset
     * @param latestVersion Latest version of the tileset available
     */
    class Available(
        val regionId: String,
        val isAsap: Boolean,
        val currentVersion: String,
        val latestVersion: String,
    ) : TilesetUpdateAvailabilityResult() {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Available

            if (isAsap != other.isAsap) return false
            if (regionId != other.regionId) return false
            if (currentVersion != other.currentVersion) return false
            if (latestVersion != other.latestVersion) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = isAsap.hashCode()
            result = 31 * result + regionId.hashCode()
            result = 31 * result + currentVersion.hashCode()
            result = 31 * result + latestVersion.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "TilesetUpdateAvailabilityResult.Available(" +
                "regionId='$regionId', " +
                "isAsap=$isAsap, " +
                "currentVersion='$currentVersion', " +
                "latestVersion='$latestVersion'" +
                ")"
        }
    }
}
