package com.mapbox.navigation.ui.tripprogress

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import java.util.Calendar

internal class TripProgressProcessor {

    fun process(action: TripProgressAction): TripProgressResult {
        return calculateTripProgress(action as TripProgressAction.CalculateTripProgress)
    }

    private fun calculateTripProgress(
        action: TripProgressAction.CalculateTripProgress
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
                100.0
            )
            else -> TripProgressResult.RouteProgressCalculation(
                eta.timeInMillis,
                action.routeProgress.distanceRemaining.toDouble(),
                action.routeProgress.currentLegProgress?.durationRemaining ?: 0.0,
                action.routeProgress.durationRemaining,
                percentRouteTraveled.toDouble()
            )
        }
    }

    private fun getPercentDistanceTraveled(routeProgress: RouteProgress): Float {
        val totalDistance = routeProgress.distanceRemaining + routeProgress.distanceTraveled
        return routeProgress.distanceTraveled / totalDistance
    }
}
