package com.mapbox.services.android.navigation.v5.utils

import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import java.util.ArrayList
import java.util.Arrays

class RouteUtils {

    companion object {
        private const val FIRST_INSTRUCTION = 0
        private const val ORIGIN_WAYPOINT_NAME_THRESHOLD = 1
        private const val ORIGIN_WAYPOINT_NAME = 0
        private const val FIRST_POSITION = 0
        private const val SECOND_POSITION = 1
        private const val SEMICOLON = ";"
    }

    /**
     * Looks at the current [RouteProgressState] and returns if
     * is [RouteProgressState.ROUTE_ARRIVED].
     *
     * @param routeProgress the current route progress
     * @return true if in arrival state, false if not
     */
    fun isArrivalEvent(routeProgress: RouteProgress): Boolean {
        val currentState = routeProgress.currentState()
        return currentState != null && currentState == RouteProgressState.ROUTE_ARRIVED
    }

    /**
     * Looks at the current [RouteProgress] list of legs and
     * checks if the current leg is the last leg.
     *
     * @param routeProgress the current route progress
     * @return true if last leg, false if not
     * @since 0.8.0
     */
    fun isLastLeg(routeProgress: RouteProgress): Boolean {
        val legs = routeProgress.directionsRoute().legs()
        val currentLeg = routeProgress.currentLeg()
        return legs?.let { legsList ->
            currentLeg == legsList[legsList.size - 1]
        } ?: false
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining coordinates
     * along the given route based on total route coordinates and the progress remaining waypoints.
     *
     *
     * If the coordinate size is less than the remaining waypoints, this method
     * will return null.
     *
     * @param routeProgress for route coordinates and remaining waypoints
     * @return list of remaining waypoints as [Point]s
     * @since 0.10.0
     */
    fun calculateRemainingWaypoints(routeProgress: RouteProgress): List<Point>? {
        val routeOptions = routeProgress.directionsRoute().routeOptions() ?: return null
        val coordinates = ArrayList(routeOptions.coordinates())
        val coordinatesSize = coordinates.size
        val remainingWaypoints = routeProgress.remainingWaypoints()
        return when (coordinatesSize < remainingWaypoints) {
            true -> null
            false -> coordinates.subList(coordinatesSize - remainingWaypoints, coordinatesSize)
        }
    }

    /**
     * Given a [RouteProgress], this method will calculate the remaining waypoint names
     * along the given route based on route option waypoint names and the progress remaining coordinates.
     *
     *
     * If the waypoint names are empty, this method will return null.
     *
     * @param routeProgress for route waypoint names and remaining coordinates
     * @return String array including the origin waypoint name and the remaining ones
     * @since 0.19.0
     */
    fun calculateRemainingWaypointNames(routeProgress: RouteProgress): Array<String?>? {
        val routeOptions = routeProgress.directionsRoute().routeOptions() ?: return null
        val allWaypointNames = routeOptions.waypointNames()
        if (allWaypointNames.isNullOrEmpty()) {
            return null
        }
        val names = allWaypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val coordinatesSize = routeOptions.coordinates().size
        val remainingWaypointNames = Arrays.copyOfRange(names,
                coordinatesSize - routeProgress.remainingWaypoints(), coordinatesSize)
        val waypointNames = arrayOfNulls<String>(remainingWaypointNames.size + ORIGIN_WAYPOINT_NAME_THRESHOLD)
        waypointNames[ORIGIN_WAYPOINT_NAME] = names[ORIGIN_WAYPOINT_NAME]
        System.arraycopy(remainingWaypointNames, FIRST_POSITION, waypointNames, SECOND_POSITION,
                remainingWaypointNames.size)
        return waypointNames
    }
}
