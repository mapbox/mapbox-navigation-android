package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Return true if the waypoint is requested explicitly. False otherwise.
 */
fun Waypoint.isRequestedWaypoint(): Boolean =
    when (this.internalType) {
        Waypoint.InternalType.Regular,
        Waypoint.InternalType.Silent,
        Waypoint.InternalType.EvChargingUser -> true
        Waypoint.InternalType.EvChargingServer -> false
    }

/**
 * Return true if the waypoint is tracked in [RouteProgress.currentLegProgress]#legIndex, based on
 * [DirectionsRoute.legs] index. False otherwise.
 */
fun Waypoint.isLegWaypoint(): Boolean =
    when (this.internalType) {
        Waypoint.InternalType.Regular,
        Waypoint.InternalType.EvChargingServer,
        Waypoint.InternalType.EvChargingUser -> true
        Waypoint.InternalType.Silent -> false
    }

/**
 * Return true if the waypoint was added by the server. False otherwise.
 */
fun Waypoint.isServerAddedWaypoint(): Boolean =
    this.internalType == Waypoint.InternalType.EvChargingServer

/**
 * Return the index of **next requested** coordinate. See [RouteOptions.coordinatesList]
 *
 * For instance, EV waypoints are not requested explicitly, so they are not taken into account.
 */
fun indexOfNextRequestedCoordinate(
    waypoints: List<Waypoint>,
    remainingWaypoints: Int,
): Int? {
    if (remainingWaypoints > waypoints.size) {
        return null
    }
    val nextWaypointIndex = waypoints.size - remainingWaypoints
    var requestedIndex = 0
    waypoints.forEachIndexed { index, waypoint ->
        if (waypoint.isRequestedWaypoint()) {
            if (index >= nextWaypointIndex) {
                return requestedIndex
            }
            requestedIndex++
        }
    }
    return null
}
