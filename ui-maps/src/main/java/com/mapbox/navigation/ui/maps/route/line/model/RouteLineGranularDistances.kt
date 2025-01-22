package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils

/**
 * Describes distance from each point on the route to the end of the route, where points are represented by [RouteLineDistancesIndex].
 *
 * Distances are represented based on calculations using [EPSG:3857 projection](https://epsg.io/3857), see [MapboxRouteLineUtils.calculateDistance].
 *
 * @param completeDistance full distance of the route
 * @param routeDistances values in this array are matching indices of all points in the full route geometry
 * @param legsDistances values in this array are matching indices of all points in each of the route legs
 * @param stepsDistances values in this array are matching indices of all points in each of the leg steps
 */
internal data class RouteLineGranularDistances constructor(
    val completeDistance: Double,
    val routeDistances: Array<RouteLineDistancesIndex>,
    val legsDistances: Array<Array<RouteLineDistancesIndex>>,
    val stepsDistances: Array<Array<Array<RouteLineDistancesIndex>>>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineGranularDistances

        if (completeDistance != other.completeDistance) return false
        if (!routeDistances.contentEquals(other.routeDistances)) return false
        if (!legsDistances.contentDeepEquals(other.legsDistances)) return false
        if (!stepsDistances.contentDeepEquals(other.stepsDistances)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = completeDistance.hashCode()
        result = 31 * result + routeDistances.contentHashCode()
        result = 31 * result + legsDistances.contentDeepHashCode()
        result = 31 * result + stepsDistances.contentDeepHashCode()
        return result
    }
}
