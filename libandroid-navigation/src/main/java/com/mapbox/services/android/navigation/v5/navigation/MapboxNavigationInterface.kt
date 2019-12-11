package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigatorInterface
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEngineFactory
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEventDispatcherInterface
import com.mapbox.services.android.navigation.v5.internal.navigation.RouteRefresherInterface
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent.FeedbackSource
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent.FeedbackType
import com.mapbox.services.android.navigation.v5.location.RawLocationListener
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera
import com.mapbox.services.android.navigation.v5.offroute.OffRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRoute
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.snap.Snap

interface MapboxNavigationInterface {
    // Interface boundary external interface access
    fun retrieveRouteRefresher(): RouteRefresherInterface

    fun retrieveMapboxNavigator(): MapboxNavigatorInterface
    fun getEventDispatcher(): NavigationEventDispatcherInterface
    fun onDestroy()

    // Interface boundary Milestone
    fun addMilestone(milestone: Milestone)

    fun addMilestones(mileStones: List<Milestone>)
    fun removeMilestone(milestone: Int)
    fun removeMilestone(milestone: Milestone)
    fun getMilestones(): List<Milestone>
    // Interface boundary LocationEngine
    fun setLocationEngine(locationEngine: LocationEngine)

    fun getLocationEngine(): LocationEngine
    fun setLocationEngineRequest(locationEngineRequest: LocationEngineRequest)
    // Interface boundary Navigation
    fun startNavigation(directionsRoute: DirectionsRoute)

    fun startNavigation(directionsRoute: DirectionsRoute, routeType: DirectionsRouteType)
    fun stopNavigation()
    // Interface boundary Listeners
    fun removeMilestoneEventListener(milestoneEventListener: MilestoneEventListener)

    fun addMilestoneEventListener(milestoneEventListener: MilestoneEventListener)
    fun addProgressChangeListener(progressChangeListener: ProgressChangeListener)
    fun removeProgressChangeListener(progressChangeListener: ProgressChangeListener)
    fun addOffRouteListener(offRouteListener: OffRouteListener)
    fun removeOffRouteListener(offRouteListener: OffRouteListener)
    fun addNavigationEventListener(navigationEventListener: NavigationEventListener)
    fun removeNavigationEventListener(navigationEventListener: NavigationEventListener)
    fun addFasterRouteListener(fasterRouteListener: FasterRouteListener)
    fun removeFasterRouteListener(fasterRouteListener: FasterRouteListener)
    fun addRawLocationListener(rawLocationListener: RawLocationListener)
    fun removeRawLocationListener(rawLocationListener: RawLocationListener)
    fun addEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener)
    fun removeEnhancedLocationListener(enhancedLocationListener: EnhancedLocationListener)
    // Interface boundary FreeDrive
    fun enableFreeDrive()

    fun disableFreeDrive()

    // Interface CameraEngine
    fun setCameraEngine(cameraEngine: Camera)

    fun getCameraEngine(): Camera?

    // Interface SnapEngine
    fun setSnapEngine(snapEngine: Snap)

    fun getSnapEngine(): Snap

    // Interface boundary OffrouteEngine
    fun setOffRouteEngine(offRouteEngine: OffRoute)

    fun getOffRouteEngine(): OffRoute

    // Interface boundary FasterRoute
    fun setFasterRouteEngine(fasterRouteEngine: FasterRoute)

    fun getFasterRouteEngine(): FasterRoute

    // Interface boundary Non-navigation
    fun recordFeedback(
        @FeedbackType feedbackType: String,
        description: String,
        @FeedbackSource source: String
    ): String

    fun updateFeedback(
        feedbackId: String,
        @FeedbackType feedbackType: String,
        description: String,
        screenshot: String
    )

    fun cancelFeedback(feedbackId: String)
    fun updateRouteLegIndex(legIndex: Int): Boolean
    fun retrieveHistory(): String
    fun toggleHistory(isEnabled: Boolean)
    fun addHistoryEvent(eventType: String, eventJsonProperties: String)
    fun retrieveSsmlAnnouncementInstruction(index: Int): String

    fun obtainAccessToken(): String
    fun getRoute(): DirectionsRoute
    fun options(): MapboxNavigationOptions
    fun retrieveEngineFactory(): NavigationEngineFactory
    fun retrieveLocationEngineRequest(): LocationEngineRequest
}
