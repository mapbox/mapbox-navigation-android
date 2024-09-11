package com.mapbox.navigation.base.route

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Represents the Match Object from Mapbox Map Matching API, which is a Route Object from
 * Mapbox Directions API that has additional fields. Additional fields could be accessed
 * here, while the part which matches the Route Object is available in [navigationRoute].
 * @param navigationRoute part of the Match Object which has the same structure as Route Object
 * @param confidence The level of confidence in the returned match, from 0 (low) to 1 (high).
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapMatchingMatch internal constructor(
    val navigationRoute: NavigationRoute,
    val confidence: Double,
) {

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = navigationRoute.hashCode()
        result = 31 * result + confidence.hashCode()
        return result
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapMatchingMatch

        if (navigationRoute != other.navigationRoute) return false
        return confidence == other.confidence
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapMatchingMatch(" +
            "confidence='$confidence'," +
            "navigationRoute=$navigationRoute" +
            ")"
    }
}
