package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import android.os.Handler
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.Date

internal class RouteProcessorRunnable(
    private val routeProcessor: NavigationRouteProcessor,
    private val navigation: MapboxNavigation,
    private val workerHandler: Handler,
    private val responseHandler: Handler,
    private val listener: RouteProcessorBackgroundThread.Listener
) : Runnable {
    private lateinit var rawLocation: Location

    companion object {
        private const val ONE_SECOND_IN_MILLISECONDS = 1000
        private const val ARRIVAL_ZONE_RADIUS = 40
    }

    override fun run() {
        if (::rawLocation.isInitialized) {
            process()
        }
    }

    fun updateRawLocation(rawLocation: Location) {
        this.rawLocation = rawLocation
    }

    private fun process() {
        // val mapboxNavigator = navigation.retrieveMapboxNavigator()
        // val options = navigation.options()
        // val route = navigation.route
        //
        // val date = Date()
        // var status = mapboxNavigator.retrieveStatus(
        //     date,
        //     options.navigationLocationEngineIntervalLagInMilliseconds().toLong()
        // )
        // val previousStatus = routeProcessor.retrievePreviousStatus()
        // status = checkForNewLegIndex(
        //     mapboxNavigator,
        //     route,
        //     status,
        //     previousStatus,
        //     options.enableAutoIncrementLegIndex()
        // )
        // val routeProgress = routeProcessor.buildNewRouteProgress(mapboxNavigator, status, route)
        // val routeRefresher = navigation.retrieveRouteRefresher()
        // ifNonNull(
        //     routeRefresher,
        //     routeProgress
        // ) { routeRefresher, routeProgress ->
        //     if (routeRefresher.check(date)) {
        //         routeRefresher.refresh(routeProgress)
        //     }
        // }
        //
        // ifNonNull(routeProgress) { routeProgress ->
        //     val engineFactory = navigation.retrieveEngineFactory()
        //     val userOffRoute =
        //         isUserOffRoute(options, status, rawLocation, routeProgress, engineFactory)
        //     val snappedLocation =
        //         findSnappedLocation(status, rawLocation, routeProgress, engineFactory)
        //     val checkFasterRoute = checkFasterRoute(
        //         options, snappedLocation, routeProgress, engineFactory,
        //         userOffRoute
        //     )
        //     val milestones = findTriggeredMilestones(navigation, routeProgress)
        //
        //     sendUpdateToResponseHandler(
        //         userOffRoute,
        //         milestones,
        //         snappedLocation,
        //         checkFasterRoute,
        //         routeProgress
        //     )
        //     routeProcessor.updatePreviousRouteProgress(routeProgress)
        //     workerHandler.postDelayed(this, ONE_SECOND_IN_MILLISECONDS.toLong())
        // }
    }

    private fun checkForNewLegIndex(
        mapboxNavigator: MapboxNavigator,
        route: DirectionsRoute,
        currentStatus: NavigationStatus,
        previousStatus: NavigationStatus?,
        autoIncrementEnabled: Boolean
    ): NavigationStatus {
        if (previousStatus == null) {
            return currentStatus
        }
        val previousState = previousStatus.routeState
        val previousLegIndex = previousStatus.legIndex
        val routeLegsSize = route.legs()?.size ?: 0
        val canUpdateLeg =
            previousState == RouteState.COMPLETE && previousLegIndex < routeLegsSize - 1
        val isValidDistanceRemaining = previousStatus.remainingLegDistance < ARRIVAL_ZONE_RADIUS
        if (autoIncrementEnabled && canUpdateLeg && isValidDistanceRemaining) {
            val newLegIndex = previousLegIndex + 1
            // return mapboxNavigator.updateLegIndex(newLegIndex)
        }
        return currentStatus
    }

    private fun isUserOffRoute(
        options: MapboxNavigationOptions,
        status: NavigationStatus,
        rawLocation: Location,
        routeProgress: RouteProgress,
        engineFactory: NavigationEngineFactory
    ): Boolean {
        val offRoute = engineFactory.retrieveOffRouteEngine()
        return if (offRoute is OffRouteDetector) {
            offRoute.isUserOffRouteWith(status)
        } else offRoute.isUserOffRoute(rawLocation, routeProgress, options)
    }

    private fun findSnappedLocation(
        status: NavigationStatus,
        rawLocation: Location,
        routeProgress: RouteProgress,
        engineFactory: NavigationEngineFactory
    ): Location {
        val snap = engineFactory.retrieveSnapEngine()
        return if (snap is SnapToRoute) {
            snap.getSnappedLocationWith(status, rawLocation)
        } else snap.getSnappedLocation(rawLocation, routeProgress)
    }

    private fun checkFasterRoute(
        options: MapboxNavigationOptions,
        rawLocation: Location,
        routeProgress: RouteProgress,
        engineFactory: NavigationEngineFactory,
        userOffRoute: Boolean
    ): Boolean {
        val fasterRoute = engineFactory.retrieveFasterRouteEngine()
        val fasterRouteDetectionEnabled = options.enableFasterRouteDetection()
        return (fasterRouteDetectionEnabled &&
            !userOffRoute &&
            fasterRoute.shouldCheckFasterRoute(rawLocation, routeProgress))
    }

    private fun findTriggeredMilestones(
        mapboxNavigation: MapboxNavigation,
        routeProgress: RouteProgress
    ): List<Milestone> {
        var previousRouteProgress = routeProcessor.retrievePreviousRouteProgress()
        if (previousRouteProgress == null) {
            previousRouteProgress = routeProgress
        }
        val milestones = ArrayList<Milestone>()
        for (milestone in mapboxNavigation.milestones) {
            if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
                milestones.add(milestone)
            }
        }
        return milestones
    }

    private fun sendUpdateToResponseHandler(
        userOffRoute: Boolean,
        milestones: List<Milestone>,
        location: Location,
        checkFasterRoute: Boolean,
        finalRouteProgress: RouteProgress
    ) {
        responseHandler.post {
            listener.onNewRouteProgress(location, finalRouteProgress)
            listener.onMilestoneTrigger(milestones, finalRouteProgress)
            listener.onUserOffRoute(location, userOffRoute)
            listener.onCheckFasterRoute(location, finalRouteProgress, checkFasterRoute)
        }
    }
}
