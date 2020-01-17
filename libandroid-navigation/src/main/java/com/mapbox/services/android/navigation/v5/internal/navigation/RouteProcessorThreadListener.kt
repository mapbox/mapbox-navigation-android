package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationHelper.buildInstructionString
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationNotificationProvider
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

internal class RouteProcessorThreadListener(
    private val eventDispatcher: NavigationEventDispatcher,
    private val routeFetcher: RouteFetcher,
    private val notificationProvider: NavigationNotificationProvider
) : RouteProcessorBackgroundThread.Listener {

    /**
     * Corresponds to ProgressChangeListener object, updating the notification and passing information
     * to the navigation event dispatcher.
     */
    override fun onNewRouteProgress(location: Location, routeProgress: RouteProgress) {
        notificationProvider.updateNavigationNotification(routeProgress)
        eventDispatcher.onProgressChange(location, routeProgress)
        eventDispatcher.onEnhancedLocationUpdate(location)
    }

    /**
     * With each valid and successful rawLocation update, this will get called once the work on the
     * navigation engine thread has finished. Depending on whether or not a milestone gets triggered
     * or not, the navigation event dispatcher will be called to notify the developer.
     */
    override fun onMilestoneTrigger(
        triggeredMilestones: List<Milestone>,
        routeProgress: RouteProgress
    ) {
        for (milestone in triggeredMilestones) {
            val instruction = buildInstructionString(routeProgress, milestone)
            eventDispatcher.onMilestoneEvent(routeProgress, instruction, milestone)
        }
    }

    /**
     * With each valid and successful rawLocation update, this callback gets invoked and depending on
     * whether or not the user is off route, the event dispatcher gets called.
     */
    override fun onUserOffRoute(location: Location, userOffRoute: Boolean) {
        when (userOffRoute) {
            true -> {
                eventDispatcher.onUserOffRoute(location)
            }
            false -> {
            }
        }
    }

    /**
     * RouteListener from the [RouteProcessorBackgroundThread] - if fired with checkFasterRoute set
     * to true, a new [DirectionsRoute] should be fetched with [RouteFetcher].
     *
     * @param location to create a new origin
     * @param routeProgress for various [com.mapbox.api.directions.v5.models.LegStep] data
     * @param checkFasterRoute true if should check for faster route, false otherwise
     */
    override fun onCheckFasterRoute(
        location: Location,
        routeProgress: RouteProgress,
        checkFasterRoute: Boolean
    ) {
        when (checkFasterRoute) {
            true -> {
                routeFetcher.findRouteFromRouteProgress(location, routeProgress)
            }
            false -> {
            }
        }
    }
}
