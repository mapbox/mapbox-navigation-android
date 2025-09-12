package com.mapbox.navigation.core.navigator.offline

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import java.util.Date

/**
 * Represents a tileset version with its release date and metadata.
 * This class encapsulates information about a specific tileset version including
 * its identifier, release date, and status flags. Version names follow the format
 * YYYY_MM_DD-HH_MM_SS which allows for lexicographic sorting to determine the latest version.
 * @param versionName The version name string in format YYYY_MM_DD-HH_MM_SS
 * @param releaseDate The release date parsed from the version name, or null if date is unknown
 * @param isLatest Whether this is the latest available version
 * @param isBlocked Whether this version is blocked and should not be used
 * @see TilesetVersionManager.getAvailableVersions for usage
 */
@ExperimentalPreviewMapboxNavigationAPI
class TilesetVersion internal constructor(
    val versionName: String,
    val releaseDate: Date?,
    val isLatest: Boolean,
    val isBlocked: Boolean,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TilesetVersion

        if (isLatest != other.isLatest) return false
        if (isBlocked != other.isBlocked) return false
        if (versionName != other.versionName) return false
        if (releaseDate != other.releaseDate) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = isLatest.hashCode()
        result = 31 * result + isBlocked.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + (releaseDate?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TilesetVersion(" +
            "versionName='$versionName', " +
            "releaseDate=$releaseDate, " +
            "isLatest=$isLatest, " +
            "isBlocked=$isBlocked" +
            ")"
    }
}
