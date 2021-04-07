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
    private var waypointArrived = false

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

        val arrivalOptions = arrivalController.arrivalOptions()
        val arrivalProgress = toArrivalProgress(arrivalOptions, routeProgress, routeLegProgress)
        val isComplete = routeProgress.currentState == RouteProgressState.ROUTE_COMPLETE
        if (isComplete && !arrivalProgress.hasMoreLegs) {
            doOnFinalDestinationArrival(routeProgress)
        } else if (isComplete && arrivalProgress.hasMoreLegs) {
            doOnWaypointArrival(arrivalProgress)
        } else if (arrivalOptions.arrivalInSeconds != null && arrivalProgress.hasMoreLegs) {
            checkWaypointArrivalTime(arrivalProgress)
        } else if (arrivalOptions.arrivalInMeters != null && arrivalProgress.hasMoreLegs) {
            checkWaypointArrivalDistance(arrivalProgress)
        }
        if (arrivalProgress.hasMoreLegs) {
            waypointArrived = isComplete
        } else {
            finalDestinationArrived = isComplete
        }
    }

    private fun toArrivalProgress(
        arrivalOptions: ArrivalOptions,
        routeProgress: RouteProgress,
        routeLegProgress: RouteLegProgress
    ): ArrivalProgress {
        return ArrivalProgress(
            arrivalOptions = arrivalOptions,
            hasMoreLegs = hasMoreLegs(routeProgress),
            routeProgress = routeProgress,
            routeLegProgress = routeLegProgress
        )
    }

    private fun hasMoreLegs(routeProgress: RouteProgress): Boolean {
        val currentLegIndex = routeProgress.currentLegProgress?.legIndex
        val lastLegIndex = routeProgress.route.legs()?.lastIndex
        return (currentLegIndex != null && lastLegIndex != null) && currentLegIndex < lastLegIndex
    }

    private fun checkWaypointArrivalTime(arrivalProgress: ArrivalProgress) {
        val arrivalInSeconds = arrivalProgress.arrivalOptions.arrivalInSeconds!!
        if (arrivalProgress.routeLegProgress.durationRemaining <= arrivalInSeconds) {
            doOnWaypointArrival(arrivalProgress)
        }
    }

    private fun checkWaypointArrivalDistance(arrivalProgress: ArrivalProgress) {
        val arrivalInMeters = arrivalProgress.arrivalOptions.arrivalInMeters!!
        if (arrivalProgress.routeLegProgress.distanceRemaining <= arrivalInMeters) {
            doOnWaypointArrival(arrivalProgress)
        }
    }

    private fun doOnWaypointArrival(arrivalProgress: ArrivalProgress) {
        if (!waypointArrived) {
            arrivalObservers.forEach { it.onWaypointArrival(arrivalProgress.routeProgress) }
        }
        val routeLegProgress = arrivalProgress.routeLegProgress
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

private class ArrivalProgress(
    val arrivalOptions: ArrivalOptions,
    val hasMoreLegs: Boolean,
    val routeProgress: RouteProgress,
    val routeLegProgress: RouteLegProgress
)
