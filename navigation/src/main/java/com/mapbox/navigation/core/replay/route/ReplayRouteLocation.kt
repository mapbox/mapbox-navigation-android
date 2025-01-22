package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.replay.MapboxReplayer

internal data class ReplayRouteSegment(
    val startSpeedMps: Double,
    val maxSpeedMps: Double,
    val endSpeedMps: Double,
    val totalDistance: Double,
    val speedUpDistance: Double,
    val cruiseDistance: Double,
    val slowDownDistance: Double,
    val steps: List<ReplayRouteStep>,
)

internal data class ReplayRouteStep(
    val timeSeconds: Double,
    val acceleration: Double,
    val speedMps: Double,
    val positionMeters: Double,
)

internal class ReplayRouteLocation(
    val routeIndex: Int?,
    val point: Point,
) {

    /**
     * [MapboxReplayer] is in Double seconds and [Location] is in milliseconds.
     * Keep track of the mapping in between
     */
    var timeMillis: Double = 0.0
    val timeSeconds: Double
        get() = timeMillis / 1000.0

    /**
     * The [ReplayRouteDriver] passes a [ReplayRouteLocation] around to estimate
     * these values at various stages. The distance is how far away the next
     * smooth route location is.
     */
    var speedMps: Double = 0.0
    var bearing: Double = 0.0
    var distance: Double = 0.0

    override fun toString(): String {
        return "ReplayRouteLocation(" +
            "routeIndex=$routeIndex, " +
            "point=$point, " +
            "timeMillis=$timeMillis, " +
            "speedMps=$speedMps, " +
            "bearing=$bearing, " +
            "distance=$distance)"
    }
}
