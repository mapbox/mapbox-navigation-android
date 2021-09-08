package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject

/**
 * Internal factory to build [RouteProgress] objects
 */
@ExperimentalMapboxNavigationAPI
object RouteProgressFactory {

    /**
     * Build a [RouteProgress] object
     *
     * @param route [DirectionsRoute] the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route.
     * @param bannerInstructions [BannerInstructions] current instructions for visual guidance.
     * @param voiceInstructions [VoiceInstructions] current instruction for audio guidance.
     * @param currentState [RouteProgressState] the current state of progress along the route.
     * Provides route and location tracking information.
     * @param currentLegProgress [RouteLegProgress] current progress of the active leg, includes
     * time and distance estimations.
     * @param upcomingStepPoints [List][Point] location coordinates describing the upcoming step.
     * @param inTunnel [Boolean] value indicating whether the current location is in a tunnel.
     * @param distanceRemaining [Float] provides the distance remaining in meters until the user
     * reaches the end of the route.
     * @param distanceTraveled [Float] representing the distance traveled along the route in meters.
     * @param durationRemaining [Double] seconds time remaining until the route destination is reached.
     * @param fractionTraveled [Float] fraction traveled along the current route. This value is
     * between 0 and 1 and isn't guaranteed to reach 1 before the user reaches the end of the route.
     * @param remainingWaypoints [Int] number of waypoints remaining on the current route.
     * @param upcomingRoadObjects list of upcoming road objects.
     * @param stale `true` if there were no location updates for a significant amount which causes
     * a lack of confidence in the progress updates being sent.
     * @param zLevel [Int] current Z-level. Can be used to to build route from proper level of road.
     */
    fun buildRouteProgressObject(
        route: DirectionsRoute,
        bannerInstructions: BannerInstructions?,
        voiceInstructions: VoiceInstructions?,
        currentState: RouteProgressState,
        currentLegProgress: RouteLegProgress?,
        upcomingStepPoints: List<Point>?,
        inTunnel: Boolean,
        distanceRemaining: Float,
        distanceTraveled: Float,
        durationRemaining: Double,
        fractionTraveled: Float,
        remainingWaypoints: Int,
        upcomingRoadObjects: List<UpcomingRoadObject>,
        stale: Boolean,
        zLevel: Int?,
    ): RouteProgress {
        return RouteProgress(
            route = route,
            bannerInstructions = bannerInstructions,
            voiceInstructions = voiceInstructions,
            currentState = currentState,
            currentLegProgress = currentLegProgress,
            upcomingStepPoints = upcomingStepPoints,
            inTunnel = inTunnel,
            distanceRemaining = distanceRemaining,
            distanceTraveled = distanceTraveled,
            durationRemaining = durationRemaining,
            fractionTraveled = fractionTraveled,
            remainingWaypoints = remainingWaypoints,
            upcomingRoadObjects = upcomingRoadObjects,
            stale = stale,
            zLevel = zLevel,
        )
    }
}
