package com.mapbox.navigation.core.arrival

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
    private var finalDestinationArrived = false

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
            val navigationStatus = tripSession.updateLegIndex(nextLegIndex)
            nextLegIndex == navigationStatus.legIndex
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

        val arrivalOptions = arrivalController.arrivalOptions()
        val hasMoreLegs = hasMoreLegs(routeProgress)
        if (routeProgress.currentState == RouteProgressState.ROUTE_COMPLETE && !hasMoreLegs) {
            doOnFinalDestinationArrival(routeProgress)
        } else if (routeProgress.currentState == RouteProgressState.ROUTE_COMPLETE && hasMoreLegs) {
            doOnWaypointArrival(routeLegProgress)
        } else if (arrivalOptions.arrivalInSeconds != null && hasMoreLegs) {
            checkWaypointArrivalTime(arrivalOptions.arrivalInSeconds, routeLegProgress)
        } else if (arrivalOptions.arrivalInMeters != null && hasMoreLegs) {
            checkWaypointArrivalDistance(arrivalOptions.arrivalInMeters, routeLegProgress)
        }
        if (!hasMoreLegs) {
            finalDestinationArrived = routeProgress.currentState == RouteProgressState.ROUTE_COMPLETE
        }
    }

    private fun hasMoreLegs(routeProgress: RouteProgress): Boolean {
        val currentLegIndex = routeProgress.currentLegProgress?.legIndex
        val lastLegIndex = routeProgress.route.legs()?.lastIndex
        return (currentLegIndex != null && lastLegIndex != null) && currentLegIndex < lastLegIndex
    }

    private fun checkWaypointArrivalTime(arrivalInSeconds: Double, routeLegProgress: RouteLegProgress) {
        if (routeLegProgress.durationRemaining <= arrivalInSeconds) {
            doOnWaypointArrival(routeLegProgress)
        }
    }

    private fun checkWaypointArrivalDistance(arrivalInMeters: Double, routeLegProgress: RouteLegProgress) {
        if (routeLegProgress.distanceRemaining <= arrivalInMeters) {
            doOnWaypointArrival(routeLegProgress)
        }
    }

    private fun doOnWaypointArrival(routeLegProgress: RouteLegProgress) {
        val moveToNextLeg = arrivalController.navigateNextRouteLeg(routeLegProgress)
        if (moveToNextLeg) {
            navigateNextRouteLeg()
        }
    }

    private fun doOnFinalDestinationArrival(routeProgress: RouteProgress) {
        if (!finalDestinationArrived) {
            arrivalObservers.forEach { it.onFinalDestinationArrival(routeProgress) }
        }
    }
}
