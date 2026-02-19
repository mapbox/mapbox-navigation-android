package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Contains data that describes how route was refreshed.
 * @param isUpToDate indicates if route was successfully refreshed recently.
 * @param experimentalProperties experimental properties (including EV data) used during
 * this refresh request. These properties were sent to the server when the route was refreshed.
 * Can be used to correlate refresh responses with the input parameters that produced them.
 */
@ExperimentalMapboxNavigationAPI
class RouteRefreshMetadata internal constructor(
    val isUpToDate: Boolean,
    val experimentalProperties: Map<String, String>? = null,
) {

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = isUpToDate.hashCode()
        result = 31 * result + (experimentalProperties?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshMetadata

        if (isUpToDate != other.isUpToDate) return false
        if (experimentalProperties != other.experimentalProperties) return false

        return true
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshMetadata(" +
            "isUpToDate=$isUpToDate, " +
            "experimentalProperties=$experimentalProperties" +
            ")"
    }
}
