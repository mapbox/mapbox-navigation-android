package com.mapbox.navigation.tripdata.progress.model

import com.mapbox.api.directions.v5.models.RouteLeg

/**
 * Represents a trip detail for the entire route to be rendered
 *
 * @param routeLegTripDetail list of trip details for each [RouteLeg]
 * @param totalTime the total time
 * @param totalDistance the total distance
 * @param totalEstimatedTimeToArrival the total estimated arrival time
 * @param formatter an object containing various types of formatters
 */
class TripOverviewValue internal constructor(
    val routeLegTripDetail: List<RouteLegTripOverview>,
    val totalTime: Double,
    val totalDistance: Double,
    val totalEstimatedTimeToArrival: Long,
    val formatter: TripProgressUpdateFormatter,
)
