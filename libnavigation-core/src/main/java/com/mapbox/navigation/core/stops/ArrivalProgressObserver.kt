package com.mapbox.navigation.core.stops

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSession
import java.util.concurrent.CopyOnWriteArraySet

internal class ArrivalProgressObserver(
    private val tripSession: TripSession
) : RouteProgressObserver {

    private var arrivalController: ArrivalController = AutoArrivalController()
    private val arrivalObservers = CopyOnWriteArraySet<ArrivalObserver>()
    private var arrivedForRoute = false

    fun attach(arrivalController: ArrivalController) {
        this.arrivalController = arrivalController
    }

    fun registerObserver(arrivalObserver: ArrivalObserver) {
        arrivalObservers.add(arrivalObserver)
    }

    fun unregisterObserver(arrivalObserver: ArrivalObserver) {
        arrivalObservers.remove(arrivalObserver)
    }

    fun navigateNextRouteLeg(): Boolean {
        val routeProgress = tripSession.getRouteProgress()
        val numberOfLegs = routeProgress?.route()?.legs()?.size
            ?: return false
        val legProgress = routeProgress.currentLegProgress()
        val legIndex = legProgress?.legIndex()
            ?: return false
        val nextLegIndex = legIndex + 1
        val nextLegStarted = if (nextLegIndex < numberOfLegs) {
            val navigationStatus = tripSession.updateLegIndex(nextLegIndex)
            return nextLegIndex == navigationStatus.legIndex
        } else {
            false
        }
        if (nextLegStarted) {
            arrivalObservers.forEach { it.onStopArrival(legProgress) }
        }
        return nextLegStarted
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
        val moveToNextLeg = arrivalController.navigateNextRouteLeg(routeLegProgress)
        if (moveToNextLeg) {
            navigateNextRouteLeg()
        }
    }

    private fun doOnRouteArrival(routeProgress: RouteProgress) {
        if (!arrivedForRoute) {
            arrivalObservers.forEach { it.onRouteArrival(routeProgress) }
        }
    }
}
