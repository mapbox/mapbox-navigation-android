package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

private data class CurrentData(
    val routeId: String,
    val legIndex: Int,
    val stepIndex: Int,
    val distances: List<Double>
)

internal class StepDistanceCache {

    private var currentData: CurrentData? = null

    fun onRouteProgressUpdate(routeProgress: RouteProgress) {
        if (routeProgress.navigationRoute.id != currentData?.routeId) {
            currentData = null
        }
        val legIndex = routeProgress.currentLegProgress?.legIndex
        val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
        if (
            legIndex != null && stepIndex != null &&
            (legIndex != currentData?.legIndex || stepIndex != currentData?.stepIndex)
        ) {
            currentData = CurrentData(
                routeProgress.navigationRoute.id,
                legIndex,
                stepIndex,
                routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints
                    ?.zipWithNext { a, b ->
                        TurfMeasurement.distance(a, b, TurfConstants.UNIT_METERS)
                    } ?: emptyList()
            )
        }
    }

    fun currentDistances(): List<Double>? = currentData?.distances
}
