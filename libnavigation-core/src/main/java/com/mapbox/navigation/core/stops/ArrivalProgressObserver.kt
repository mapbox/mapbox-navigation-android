package com.mapbox.navigation.core.stops

import com.mapbox.navigation.base.trip.RouteProgressObserver
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.TripSession

internal class ArrivalProgressObserver(
    private val tripSession: TripSession
) : RouteProgressObserver {

    private var arrivalController: ArrivalController = AutoArrivalController()
    private var arrivedForRoute = false

    fun attach(arrivalController: ArrivalController) {
        this.arrivalController = arrivalController
    }

    fun navigateNextRouteLeg(): Boolean {
        val numberOfLegs = tripSession.getRouteProgress()?.route()?.legs()?.size
            ?: return false
        val legIndex = tripSession.getRouteProgress()?.currentLegProgress()?.legIndex()
            ?: return false
        val nextLegIndex = legIndex + 1
        return if (nextLegIndex < numberOfLegs) {
            val navigationStatus = tripSession.updateLegIndex(nextLegIndex)
            return nextLegIndex == navigationStatus.legIndex
        } else {
            true
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val routeLegProgress = routeProgress.currentLegProgress()
            ?: return

        val arrivalOptions = arrivalController.arrivalOptions()
        if (routeProgress.currentState() == RouteProgressState.ROUTE_ARRIVED && !hasMoreLegs(routeProgress)) {
            doOnRouteArrival(routeProgress)
        } else if (arrivalOptions.arrivalInSeconds != null) {
            checkStopArrivalTime(arrivalOptions.arrivalInSeconds, routeLegProgress)
        } else if (arrivalOptions.arrivalInMeters != null) {
            checkStopArrivalDistance(arrivalOptions.arrivalInMeters, routeLegProgress)
        }
        arrivedForRoute = (routeProgress.currentState() ?: RouteProgressState.ROUTE_UNCERTAIN) == RouteProgressState.ROUTE_ARRIVED
    }

    private fun hasMoreLegs(routeProgress: RouteProgress): Boolean {
        val currentLegIndex = routeProgress.currentLegProgress()?.legIndex()
        val lastLegIndex = routeProgress.route()?.legs()?.lastIndex
        return (currentLegIndex != null && lastLegIndex != null) && currentLegIndex < lastLegIndex
    }

    private fun checkStopArrivalTime(arrivalInSeconds: Double, routeLegProgress: RouteLegProgress) {
        if (routeLegProgress.durationRemaining() <= arrivalInSeconds) {
            doOnStopArrival(routeLegProgress)
        }
    }

    private fun checkStopArrivalDistance(arrivalInMeters: Double, routeLegProgress: RouteLegProgress) {
        if (routeLegProgress.distanceRemaining() <= arrivalInMeters) {
            doOnStopArrival(routeLegProgress)
        }
    }

    private fun doOnStopArrival(routeLegProgress: RouteLegProgress) {
        val moveToNextLeg = arrivalController.onStopArrival(routeLegProgress)
        if (moveToNextLeg) {
            navigateNextRouteLeg()
        }
    }

    private fun doOnRouteArrival(routeProgress: RouteProgress) {
        if (!arrivedForRoute) {
            arrivalController.onRouteArrival(routeProgress)
        }
    }
}
