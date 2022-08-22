package com.mapbox.navigation.ui.maps.route.line.model

/**
 * @param distance full distance of the route
 * @param distancesArray array where index is the index of the upcoming not yet visited point on the route
 */
internal data class RouteLineGranularDistances(
    val distance: Double,
    val distancesArray: Array<RouteLineDistancesIndex>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteLineGranularDistances) return false

        if (distance != other.distance) return false
        if (!distancesArray.contentEquals(other.distancesArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = distance.hashCode()
        result = 31 * result + distancesArray.contentHashCode()
        return result
    }
}
