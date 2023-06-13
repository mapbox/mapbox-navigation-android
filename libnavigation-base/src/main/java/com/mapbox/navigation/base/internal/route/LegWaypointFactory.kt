package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonElement
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.LegWaypoint

object LegWaypointFactory {

    fun createLegWaypoint(
        location: Point,
        name: String,
        target: Point?,
        @LegWaypoint.Type type: String,
        metadata: Map<String, JsonElement>?,
    ): LegWaypoint = LegWaypoint(location, name, target, type, metadata)
}
