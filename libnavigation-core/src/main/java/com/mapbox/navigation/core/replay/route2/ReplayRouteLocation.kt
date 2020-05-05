package com.mapbox.navigation.core.replay.route2

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.replay.history.ReplayHistoryPlayer

internal data class ReplayRouteSegment(
    val startSpeedMps: Double,
    val endSpeedMps: Double,
    val distanceMeters: Double,
    val steps: List<ReplayRouteStep>
)

internal data class ReplayRouteStep(
    val acceleration: Double,
    val speedMps: Double,
    val positionMeters: Double
)

internal class ReplayRouteLocation(
    val routeIndex: Int?,
    val point: Point
) {

    /**
     * [ReplayHistoryPlayer] is in Double seconds and [Location] is in milliseconds.
     * Keep track of the mapping in between
     */
    var timeMillis: Long = 0L
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
            "point=$point, " +
            "timeMillis=$timeMillis, " +
            "speedMps=$speedMps, " +
            "bearing=$bearing, " +
            "distance=$distance)"
    }
}
