package com.mapbox.navigation.tripdata.progress.model

import com.mapbox.api.directions.v5.models.RouteLeg

/**
 * Represents the trip detail for a [RouteLeg]
 *
 * @param legIndex index of [RouteLeg]
 * @param legTime time remaining for the [RouteLeg]
 * @param legDistance distance remaining for the [RouteLeg]
 * @param estimatedTimeToArrival the estimated time to arrival for the [RouteLeg]
 */
class RouteLegTripOverview internal constructor(
    val legIndex: Int,
    val legTime: Double,
    val legDistance: Double,
    val estimatedTimeToArrival: Long,
)
