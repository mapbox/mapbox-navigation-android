package com.mapbox.navigation.tripdata.progress.model

import androidx.annotation.ColorInt
import java.util.TimeZone

/**
 * Represents a trip progress update to be rendered
 *
 * @param estimatedTimeToArrival the estimated time to arrival
 * @param arrivalTimeZone the arrival time zone for the route
 * @param distanceRemaining the distance remaining
 * @param currentLegTimeRemaining the time remaining for the current leg
 * @param totalTimeRemaining the total time remaining
 * @param percentRouteTraveled the percentage of the route traveled
 * @param trafficCongestionColor reserved for future use
 * @param formatter an object containing various types of formatters
 */
class TripProgressUpdateValue internal constructor(
    val estimatedTimeToArrival: Long,
    val arrivalTimeZone: TimeZone?,
    val distanceRemaining: Double,
    val currentLegTimeRemaining: Double,
    val totalTimeRemaining: Double,
    val percentRouteTraveled: Double,
    @ColorInt val trafficCongestionColor: Int,
    val formatter: TripProgressUpdateFormatter,
)
