package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject

/**
 * Internal factory to build [RouteProgress] objects
 */
object RouteProgressInstanceFactory {

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
     */
    fun buildRouteProgressObject(
        route: DirectionsRoute,
        bannerInstructions: BannerInstructions? = null,
        voiceInstructions: VoiceInstructions? = null,
        currentState: RouteProgressState = RouteProgressState.INITIALIZED,
        currentLegProgress: RouteLegProgress? = null,
        upcomingStepPoints: List<Point>? = null,
        inTunnel: Boolean = false,
        distanceRemaining: Float = 0f,
        distanceTraveled: Float = 0f,
        durationRemaining: Double = 0.0,
        fractionTraveled: Float = 0f,
        remainingWaypoints: Int = 0,
        upcomingRoadObjects: List<UpcomingRoadObject> = emptyList(),
        stale: Boolean = false
    ): RouteProgress {
        return RouteProgress(
            route,
            bannerInstructions,
            voiceInstructions,
            currentState,
            currentLegProgress,
            upcomingStepPoints,
            inTunnel,
            distanceRemaining,
            distanceTraveled,
            durationRemaining,
            fractionTraveled,
            remainingWaypoints,
            upcomingRoadObjects,
            stale
        )
    }
}
