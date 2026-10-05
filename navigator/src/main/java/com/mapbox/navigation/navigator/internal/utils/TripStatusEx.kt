@file:JvmName("TripStatusEx")

package com.mapbox.navigation.navigator.internal.utils

import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.route.LegWaypointFactory
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigator.RouteState
import kotlin.math.max

private const val INDEX_OF_INITIAL_LEG_TARGET = 1

fun TripStatus.calculateRemainingWaypoints(): Int {
    val routeWaypoints = this.route?.internalWaypoints()
    return if (routeWaypoints != null) {
        val waypointsCount = routeWaypoints.size
        val nextWaypointIndex = normalizeNextWaypointIndex(
            this.navigationStatus.nextWaypointIndex,
        )
        return waypointsCount - nextWaypointIndex
    } else {
        0
    }
}

fun TripStatus.getCurrentLegDestination(route: NavigationRoute): LegWaypoint? {
    val nextWaypointIndex = normalizeNextWaypointIndex(
        this.navigationStatus.nextWaypointIndex,
    )
    val waypoint = route.internalWaypoints().drop(nextWaypointIndex).firstOrNull {
        it.isLegWaypoint()
    }
    if (waypoint == null) {
        return null
    }
    val legWaypointType = when (waypoint.type) {
        Waypoint.REGULAR -> LegWaypoint.REGULAR
        Waypoint.EV_CHARGING_SERVER -> LegWaypoint.EV_CHARGING_ADDED
        Waypoint.EV_CHARGING_USER -> LegWaypoint.EV_CHARGING_USER_PROVIDED
        else -> throw IllegalArgumentException("$waypoint is not a leg waypoint")
    }
    return LegWaypointFactory.createLegWaypoint(
        waypoint.location,
        waypoint.name,
        waypoint.target,
        legWaypointType,
        waypoint.metadata,
    )
}

/**
 * On the Android side, we always start navigation from the current position.
 * So we expect that the next waypoint index will not be less than 1.
 * But the native part considers the origin as a usual waypoint.
 * It can return the next waypoint index 0. Be careful, this case isn't easy to reproduce.
 *
 * For example, nextWaypointIndex=0 leads to an incorrect rerouting.
 * We don't want to get to an initial position even it hasn't been reached yet.
 *
 * Exception: while charging at the origin (`routeState == INITIALIZED` and waypoint 0 is an
 * EV charging waypoint), `nextWaypointIndex = 0` is kept as-is, so the charging station stays
 * reported as an unvisited waypoint instead of being reported as already passed.
 */
private fun TripStatus.normalizeNextWaypointIndex(nextWaypointIndex: Int) = if (
    isChargingAtOrigin(nextWaypointIndex)
) {
    nextWaypointIndex
} else {
    max(INDEX_OF_INITIAL_LEG_TARGET, nextWaypointIndex)
}

/**
 * `nextWaypointIndex = 0` is reported unconditionally by native whenever waypoint 0 is a CPOI
 * (see NN-4059), regardless of whether charging is actually needed there - so this stays
 * waypoint-type-based rather than keying off [TripStatus.chargingState]. The app-facing signal
 * for "is charging actually expected" is [com.mapbox.navigation.base.trip.model.RouteProgress.isChargingExpected],
 * which is deliberately based on the native charging state machine instead (see its KDoc):
 * a route can start at a charging waypoint whose charge is already sufficient, in which case
 * native's charging state machine stays `NotCharging` and, per the RFC, the driver isn't
 * expected to take any explicit action there.
 */
private fun TripStatus.isChargingAtOrigin(nextWaypointIndex: Int): Boolean {
    if (nextWaypointIndex != 0) return false
    if (navigationStatus.routeState != RouteState.INITIALIZED) return false
    val originWaypoint = route?.internalWaypoints()?.firstOrNull() ?: return false
    return originWaypoint.type == Waypoint.EV_CHARGING_SERVER ||
        originWaypoint.type == Waypoint.EV_CHARGING_USER
}
