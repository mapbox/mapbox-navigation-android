package com.mapbox.navigation.tripdata.progress

internal sealed class TripProgressResult {
    data class RouteProgressCalculation(
        val estimatedTimeToArrival: Long,
        val distanceRemaining: Double,
        val currentLegTimeRemaining: Double,
        val totalTimeRemaining: Double,
        val percentRouteTraveled: Double,
    ) : TripProgressResult()

    sealed class TripOverview : TripProgressResult() {
        data class RouteLegTripOverview(
            val legIndex: Int,
            val legTime: Double,
            val legDistance: Double,
            val estimatedTimeToArrival: Long,
        )

        data class Success(
            val routeLegTripDetail: List<RouteLegTripOverview>,
            val totalTime: Double,
            val totalDistance: Double,
            val totalEstimatedTimeToArrival: Long,
        ) : TripOverview()

        data class Failure(
            val errorMessage: String?,
            val throwable: Throwable?,
        ) : TripOverview()
    }
}
