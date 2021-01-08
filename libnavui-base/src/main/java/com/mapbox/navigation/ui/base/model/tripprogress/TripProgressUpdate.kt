package com.mapbox.navigation.ui.base.model.tripprogress

import androidx.annotation.ColorInt

/**
 * TripProgressUpdate
 *
 * @param estimatedTimeToArrival a value in milliseconds representing a date and time
 * @param distanceRemaining the distance remaining in meters
 * @param currentLegTimeRemaining the time remaining in the current leg in seconds
 * @param totalTimeRemaining the total route time remaining in seconds
 * @param percentRouteTraveled the percent route distance traveled
 * @param trafficCongestionColor reserved for future use, will be a value of -1
 */
class TripProgressUpdate(
    val estimatedTimeToArrival: Long,
    val distanceRemaining: Double,
    val currentLegTimeRemaining: Double,
    val totalTimeRemaining: Double,
    val percentRouteTraveled: Double,
    @ColorInt val trafficCongestionColor: Int
)
