package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Contains data that describes how route was refreshed.
 * @param isUpToDate indicates if route was successfully refreshed recently.
 */
@ExperimentalMapboxNavigationAPI
class RouteRefreshMetadata internal constructor(
    val isUpToDate: Boolean,
) {

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return isUpToDate.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshMetadata

        return isUpToDate == other.isUpToDate
    }
}
