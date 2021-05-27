package com.mapbox.navigation.core.arrival

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
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
    private var routeArrived: DirectionsRoute? = null
    private var routeLegArrived: RouteLeg? = null

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
        val numberOfLegs = routeProgress?.route?.legs()?.size
            ?: return false
        val legProgress = routeProgress.currentLegProgress
        val legIndex = legProgress?.legIndex
            ?: return false
        val nextLegIndex = legIndex + 1
        val nextLegStarted = if (nextLegIndex < numberOfLegs) {
            tripSession.updateLegIndex(nextLegIndex)
        } else {
            false
        }
        if (nextLegStarted) {
            arrivalObservers.forEach { it.onNextRouteLegStart(legProgress) }
        }
        return nextLegStarted
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val routeLegProgress = routeProgress.currentLegProgress
            ?: return

        if (routeProgress.currentState != RouteProgressState.COMPLETE || routeProgress.stale) {
            return
        }

        val hasMoreLegs = hasMoreLegs(routeProgress)
        if (hasMoreLegs) {
            doOnWaypointArrival(routeProgress, routeLegProgress)
        } else if (!hasMoreLegs) {
            doFinalDestinationArrival(routeProgress)
        }
    }

    private fun hasMoreLegs(routeProgress: RouteProgress): Boolean {
        val currentLegIndex = routeProgress.currentLegProgress?.legIndex
        val lastLegIndex = routeProgress.route.legs()?.lastIndex
        return (currentLegIndex != null && lastLegIndex != null) && currentLegIndex < lastLegIndex
    }

    private fun doOnWaypointArrival(
        routeProgress: RouteProgress,
        routeLegProgress: RouteLegProgress
    ) {
        if (routeLegArrived != routeLegProgress.routeLeg) {
            routeLegArrived = routeLegProgress.routeLeg
            arrivalObservers.forEach { it.onWaypointArrival(routeProgress) }
        }
        val moveToNextLeg = arrivalController.navigateNextRouteLeg(routeLegProgress)
        if (moveToNextLeg) {
            navigateNextRouteLeg()
        }
    }

    private fun doFinalDestinationArrival(routeProgress: RouteProgress) {
        if (routeArrived != routeProgress.route) {
            routeArrived = routeProgress.route
            arrivalObservers.forEach { it.onFinalDestinationArrival(routeProgress) }
        }
    }
}
