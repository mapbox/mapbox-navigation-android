package com.mapbox.navigation.tripdata.progress

import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import kotlin.time.Duration.Companion.seconds

internal class TripProgressProcessor {

    fun process(action: TripProgressAction): TripProgressResult {
        return when (action) {
            is TripProgressAction.CalculateTripDetails -> {
                calculateTripDetails(action)
            }
            is TripProgressAction.CalculateTripProgress -> {
                calculateTripProgress(action)
            }
        }
    }

    private fun calculateTripDetails(
        action: TripProgressAction.CalculateTripDetails,
    ): TripProgressResult.TripOverview {
        val legWaypoints = action.route.internalWaypoints().filter { it.isLegWaypoint() }
        val directionsRoute = action.route.directionsRoute
        val legs = directionsRoute.legs() ?: return TripProgressResult.TripOverview.Failure(
            errorMessage = "Directions route should not have null RouteLegs",
            throwable = null,
        )
        var eta = System.currentTimeMillis()
        val tripDetailsList = legs.mapIndexed { index, leg ->
            val duration = leg.duration() ?: return TripProgressResult.TripOverview.Failure(
                errorMessage = "RouteLeg duration cannot be null",
                throwable = null,
            )
            val distance = leg.distance() ?: return TripProgressResult.TripOverview.Failure(
                errorMessage = "RouteLeg distance cannot be null",
                throwable = null,
            )
            eta += duration.seconds.inWholeMilliseconds
            TripProgressResult.TripOverview.RouteLegTripOverview(
                index,
                duration,
                distance,
                eta,
                legWaypoints.getOrNull(index + 1)?.timeZone?.toJavaTimeZone(),
            )
        }
        val totalTime = directionsRoute.duration()
        return TripProgressResult.TripOverview.Success(
            routeLegTripDetail = tripDetailsList,
            totalTime = totalTime,
            totalDistance = directionsRoute.distance(),
            totalEstimatedTimeToArrival = System.currentTimeMillis() +
                totalTime.seconds.inWholeMilliseconds,
            legWaypoints.lastOrNull()?.timeZone?.toJavaTimeZone(),
        )
    }

    private fun calculateTripProgress(
        action: TripProgressAction.CalculateTripProgress,
    ): TripProgressResult.RouteProgressCalculation {
        val arrivalTimeZone = action.routeProgress.navigationRoute.internalWaypoints()
            .lastOrNull()?.timeZone?.toJavaTimeZone()
        return when (action.routeProgress.currentState) {
            RouteProgressState.COMPLETE -> TripProgressResult.RouteProgressCalculation(
                System.currentTimeMillis(),
                distanceRemaining = 0.0,
                currentLegTimeRemaining = 0.0,
                totalTimeRemaining = 0.0,
                percentRouteTraveled = 100.0,
                arrivalTimeZone,
            )
            else -> TripProgressResult.RouteProgressCalculation(
                estimatedTimeToArrival = System.currentTimeMillis() +
                    action.routeProgress.durationRemaining.seconds.inWholeMilliseconds,
                action.routeProgress.distanceRemaining.toDouble(),
                action.routeProgress.currentLegProgress?.durationRemaining ?: 0.0,
                action.routeProgress.durationRemaining,
                getPercentDistanceTraveled(action.routeProgress).toDouble(),
                arrivalTimeZone,
            )
        }
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDistance = routeProgress.distanceRemaining + routeProgress.distanceTraveled
        return routeProgress.distanceTraveled / totalDistance
    }
}
