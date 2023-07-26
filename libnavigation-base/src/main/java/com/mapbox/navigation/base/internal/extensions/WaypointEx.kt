package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.serverAddsChargingStations
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Return true if the waypoint is requested explicitly. False otherwise.
 */
fun Waypoint.isRequestedWaypoint(routeOptions: RouteOptions): Boolean {
    return if (routeOptions.serverAddsChargingStations()) {
        when (this.internalType) {
            Waypoint.InternalType.Regular,
            Waypoint.InternalType.Silent,
            Waypoint.InternalType.EvChargingUser -> true
            Waypoint.InternalType.EvChargingServer -> false
        }
    } else {
        true
    }
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
 */
fun indexOfNextWaypoint(
    waypoints: List<Waypoint>,
    remainingWaypoints: Int,
): Int? {
    if (remainingWaypoints > waypoints.size || remainingWaypoints == 0) {
        return null
    }
    return waypoints.size - remainingWaypoints
}
