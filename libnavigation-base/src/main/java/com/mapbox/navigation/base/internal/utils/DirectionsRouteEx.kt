@file:JvmName("DirectionsRouteEx")

package com.mapbox.navigation.base.internal.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.utils.ifNonNull
import com.mapbox.navigation.utils.internal.logE

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
            metadata = nativeWaypoint.metadata?.parseMetadata()
        )
    }

private fun String.parseMetadata(): Map<String, JsonElement>? {
    return try {
        val map = mutableMapOf<String, JsonElement>()
        val json = JsonParser.parseString(this).asJsonObject
        for (entry in json.entrySet()) {
            map[entry.key] = entry.value
        }
        map.toMap()
    } catch (ex: Throwable) {
        logE(null) {
            "Could not parse $this to metadata: ${ex.message}"
        }
        null
    }
}

fun DirectionsRoute.refreshTtl(): Int? {
    return try {
        unrecognizedJsonProperties?.get(Constants.RouteResponse.KEY_REFRESH_TTL)?.asInt
    } catch (ex: Throwable) {
        null
    }
}

private fun com.mapbox.navigator.WaypointType.mapToSdk(): Waypoint.InternalType =
    when (this) {
        com.mapbox.navigator.WaypointType.REGULAR -> Waypoint.InternalType.Regular
        com.mapbox.navigator.WaypointType.SILENT -> Waypoint.InternalType.Silent
        com.mapbox.navigator.WaypointType.EV_CHARGING_SERVER ->
            Waypoint.InternalType.EvChargingServer
        com.mapbox.navigator.WaypointType.EV_CHARGING_USER -> Waypoint.InternalType.EvChargingUser
    }
