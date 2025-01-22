package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Historical event that represents when a route was set.
 *
 * @param eventTimestamp timestamp of event seconds
 * @param navigationRoute the route that was set, `null` when it cleared
 * @param routeIndex the index of this route
 * @param legIndex the current leg index when the route was set
 * @param profile the routing profile to use
 * @param geometries the geometry polyline encoding
 * @param waypoints the coordinates for this route
 * @param allRoutes list of all routes that were set, empty if they were cleared
 */
class HistoryEventSetRoute internal constructor(
    override val eventTimestamp: Double,
    val navigationRoute: NavigationRoute?,
    val routeIndex: Int,
    val legIndex: Int,
    @DirectionsCriteria.ProfileCriteria val profile: String,
    @DirectionsCriteria.GeometriesCriteria val geometries: String,
    val waypoints: List<HistoryWaypoint>,
    val allRoutes: List<NavigationRoute>,
) : HistoryEvent {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryEventSetRoute

        if (navigationRoute != other.navigationRoute) return false
        if (routeIndex != other.routeIndex) return false
        if (legIndex != other.legIndex) return false
        if (profile != other.profile) return false
        if (geometries != other.geometries) return false
        if (waypoints != other.waypoints) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = navigationRoute.hashCode()
        result = 31 * result + routeIndex
        result = 31 * result + legIndex
        result = 31 * result + profile.hashCode()
        result = 31 * result + geometries.hashCode()
        result = 31 * result + waypoints.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SetRouteHistoryEvent(" +
            "navigationRoute=$navigationRoute, " +
            "routeIndex=$routeIndex, " +
            "legIndex=$legIndex, " +
            "profile='$profile', " +
            "geometries='$geometries', " +
            "waypoints=$waypoints" +
            ")"
    }
}
