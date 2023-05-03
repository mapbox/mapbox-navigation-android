@file:JvmName("DirectionsRouteEx")

package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.utils.ifNonNull

/**
 * Compare routes as geometries (if exist) or as a names of [LegStep] of the [DirectionsRoute].
 *
 * **This check does not compare route annotations!**
 */
fun DirectionsRoute.isSameRoute(compare: DirectionsRoute?): Boolean {
    if (this === compare) {
        return true
    }

    if (compare == null) {
        return false
    }

    ifNonNull(this.geometry(), compare.geometry()) { g1, g2 ->
        return g1 == g2
    }

    ifNonNull(this.stepsNamesAsString(), compare.stepsNamesAsString()) { s1, s2 ->
        return s1 == s2
    }

    return false
}

private fun DirectionsRoute.stepsNamesAsString(): String? =
    this.legs()
        ?.joinToString { leg ->
            leg.steps()?.joinToString { step -> step.name() ?: "" } ?: ""
        }

internal fun List<com.mapbox.navigator.Waypoint>.mapToSdk(): List<Waypoint> =
    map { nativeWaypoint ->
        Waypoint(
            location = nativeWaypoint.location,
            internalType = nativeWaypoint.type.mapToSdk(),
            name = nativeWaypoint.name,
            target = nativeWaypoint.target,
        )
    }

private fun com.mapbox.navigator.WaypointType.mapToSdk(): Waypoint.InternalType =
    when (this) {
        com.mapbox.navigator.WaypointType.REGULAR -> Waypoint.InternalType.Regular
        com.mapbox.navigator.WaypointType.SILENT -> Waypoint.InternalType.Silent
        com.mapbox.navigator.WaypointType.EV_CHARGING_SERVER ->
            Waypoint.InternalType.EvChargingServer
        com.mapbox.navigator.WaypointType.EV_CHARGING_USER -> Waypoint.InternalType.EvChargingUser
    }
