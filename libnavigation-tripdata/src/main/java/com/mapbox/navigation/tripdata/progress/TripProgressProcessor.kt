package com.mapbox.navigation.tripdata.progress

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Calendar
import kotlin.jvm.Throws

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

    @Throws
    private fun calculateTripDetails(
        action: TripProgressAction.CalculateTripDetails,
    ): TripProgressResult.TripOverview {
        val tripDetailsList = mutableListOf<TripProgressResult.TripOverview.RouteLegTripOverview>()
        ifNonNull(action.route.directionsRoute.legs()) { legs ->
            legs.forEachIndexed { index, leg ->
                ifNonNull(leg.duration(), leg.distance()) { duration, distance ->
                    val eta = Calendar.getInstance().also {
                        it.add(Calendar.SECOND, duration.toInt())
                    }.timeInMillis
                    tripDetailsList.add(
                        TripProgressResult.TripOverview.RouteLegTripOverview(
                            legIndex = index,
                            estimatedTimeToArrival = eta,
                            legTime = duration,
                            legDistance = distance,
                        ),
                    )
                } ?: return TripProgressResult.TripOverview.Failure(
                    errorMessage = "RouteLeg duration and RouteLeg distance cannot be null",
                    throwable = null,
                )
            }
        } ?: return TripProgressResult.TripOverview.Failure(
            errorMessage = "Directions route should not have null RouteLegs",
            throwable = null,
        )
        val totalTime = action.route.directionsRoute.duration()
        val totalDistance = action.route.directionsRoute.distance()
        val totalEstimatedArrivalTime = Calendar.getInstance().also {
            it.add(Calendar.SECOND, action.route.directionsRoute.duration().toInt())
        }.timeInMillis
        return TripProgressResult.TripOverview.Success(
            routeLegTripDetail = tripDetailsList,
            totalTime = totalTime,
            totalDistance = totalDistance,
            totalEstimatedTimeToArrival = totalEstimatedArrivalTime,
        )
    }

    private fun calculateTripProgress(
        action: TripProgressAction.CalculateTripProgress,
    ): TripProgressResult.RouteProgressCalculation {
        val eta = if (action.routeProgress.currentState == RouteProgressState.COMPLETE) {
            Calendar.getInstance()
        } else {
            Calendar.getInstance().also {
                it.add(Calendar.SECOND, action.routeProgress.durationRemaining.toInt())
            }
        }
        val percentRouteTraveled = getPercentDistanceTraveled(action.routeProgress)

        return when (action.routeProgress.currentState) {
            RouteProgressState.COMPLETE -> TripProgressResult.RouteProgressCalculation(
                Calendar.getInstance().timeInMillis,
                0.0,
                0.0,
                0.0,
                100.0,
            )
            else -> TripProgressResult.RouteProgressCalculation(
                eta.timeInMillis,
                action.routeProgress.distanceRemaining.toDouble(),
                action.routeProgress.currentLegProgress?.durationRemaining ?: 0.0,
                action.routeProgress.durationRemaining,
                percentRouteTraveled.toDouble(),
            )
        }
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDistance = routeProgress.distanceRemaining + routeProgress.distanceTraveled
        return routeProgress.distanceTraveled / totalDistance
    }
}
