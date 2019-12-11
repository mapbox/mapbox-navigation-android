package com.mapbox.services.android.navigation.v5.internal.navigation

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetricListener
import com.mapbox.services.android.navigation.v5.location.RawLocationListener
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.navigation.EnhancedLocationListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

interface NavigationEventDispatcherInterface {
    fun addMilestoneEventListener(milestoneEventListener: MilestoneEventListener)

    fun removeMilestoneEventListener(milestoneEventListener: MilestoneEventListener?)

    fun addProgressChangeListener(progressChangeListener: ProgressChangeListener)

    fun removeProgressChangeListener(progressChangeListener: ProgressChangeListener?)

    fun addOffRouteListener(offRouteListener: OffRouteListener)

    fun removeOffRouteListener(offRouteListener: OffRouteListener?)

    fun addNavigationEventListener(navigationEventListener: NavigationEventListener)

    fun removeNavigationEventListener(navigationEventListener: NavigationEventListener?)

    fun addFasterRouteListener(fasterRouteListener: FasterRouteListener)

    fun removeFasterRouteListener(fasterRouteListener: FasterRouteListener?)

    fun addRawLocationListener(rawLocationListener: RawLocationListener)

    fun removeRawLocationListener(rawLocationListener: RawLocationListener?)

    fun addEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener)

    fun removeEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener?)

    fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone)

    fun onProgressChange(location: Location, routeProgress: RouteProgress)

    fun onNavigationEvent(isRunning: Boolean)

    fun onUserOffRoute(location: Location)

    fun onFasterRouteEvent(directionsRoute: DirectionsRoute)

    fun onLocationUpdate(location: Location)

    fun onEnhancedLocationUpdate(location: Location)

    fun addMetricEventListeners(eventListeners: NavigationMetricListener)
}
