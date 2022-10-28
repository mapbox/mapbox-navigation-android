package com.mapbox.navigation.navigator.internal.utils

import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.navigator.internal.TripStatus
import kotlin.math.max

private const val INDEX_OF_INITIAL_LEG_TARGET = 1

fun TripStatus.calculateRemainingWaypoints(): Int {
    val routeWaypoints = this.route?.internalWaypoints()
    return if (routeWaypoints != null) {
        val waypointsCount = routeWaypoints.size
        val nextWaypointIndex = normalizeNextWaypointIndex(
            this.navigationStatus.nextWaypointIndex
        )
        return waypointsCount - nextWaypointIndex
    } else {
        0
    }
}

/**
 * On the Android side, we always start navigation from the current position.
 * So we expect that the next waypoint index will not be less than 1.
 * But the native part considers the origin as a usual waypoint.
 * It can return the next waypoint index 0. Be careful, this case isn't easy to reproduce.
 *
 * For example, nextWaypointIndex=0 leads to an incorrect rerouting.
 * We don't want to get to an initial position even it hasn't been reached yet.
 */
private fun normalizeNextWaypointIndex(nextWaypointIndex: Int) = max(
    INDEX_OF_INITIAL_LEG_TARGET,
    nextWaypointIndex
)
