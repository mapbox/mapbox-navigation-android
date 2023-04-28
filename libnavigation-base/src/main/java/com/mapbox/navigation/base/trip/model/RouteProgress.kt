package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.trip.model.RouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints

/**
 * This class contains all progress information at any given time during a navigation session. This
 * progress includes information for the current route, leg, and step that the user is traveling along.
 * With every new valid location update, a new route progress will be generated using the latest
 * information.
 *
 * The latest route progress object can be obtained through the [RouteProgressObserver].
 * Note that the route progress object's immutable.
 *
 * @param navigationRoute [NavigationRoute] the navigation session is currently using. When a reroute occurs and a new
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
 * @param remainingWaypoints [Int] number of waypoints remaining on the current route. The waypoints number can be different
 * with number of requested coordinates. For instance, [EV routing](https://docs.mapbox.com/api/navigation/directions/#electric-vehicle-routing)
 * is adding additional waypoints, that are not requested explicitly.
 * @param upcomingRoadObjects list of upcoming road objects.
 * @param stale `true` if there were no location updates for a significant amount which causes
 * a lack of confidence in the progress updates being sent.
 * @param routeAlternativeId in case of [currentState] equal to [RouteProgressState.OFF_ROUTE],
 * this field can provide the route ID of an alternative route that user turned into causing off-route event (if there is one).
 * This field can be used to find a route with [NavigationRoute.id] that can be immediately used as the new primary route.
 * @param currentRouteGeometryIndex route-wise index representing the geometry point that starts the segment
 * the user is currently on, effectively this represents the index of last visited geometry point in the route
 * (see [DirectionsRoute.geometry] or [DecodeUtils.completeGeometryToPoints] if [RouteOptions.overview] is [DirectionsCriteria.OVERVIEW_FULL]).
 * @param alternativeRoutesIndices map of alternative route id to route indices for specified route (see [RouteIndices]). No primary route indices data is available here.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteProgress internal constructor(
    val navigationRoute: NavigationRoute,
    val bannerInstructions: BannerInstructions?,
    val voiceInstructions: VoiceInstructions?,
    val currentState: RouteProgressState,
    val currentLegProgress: RouteLegProgress?,
    val upcomingStepPoints: List<Point>?,
    val inTunnel: Boolean,
    val distanceRemaining: Float,
    val distanceTraveled: Float,
    val durationRemaining: Double,
    val fractionTraveled: Float,
    val remainingWaypoints: Int,
    val upcomingRoadObjects: List<UpcomingRoadObject>,
    val stale: Boolean,
    val routeAlternativeId: String?,
    val currentRouteGeometryIndex: Int,
    internal val alternativeRoutesIndices: Map<String, RouteIndices>,
) {

    /**
     * [DirectionsRoute] the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route.
     */
    val route: DirectionsRoute
        get() = navigationRoute.directionsRoute

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteProgress

        if (navigationRoute != other.navigationRoute) return false
        if (bannerInstructions != other.bannerInstructions) return false
        if (voiceInstructions != other.voiceInstructions) return false
        if (currentState != other.currentState) return false
        if (currentLegProgress != other.currentLegProgress) return false
        if (upcomingStepPoints != other.upcomingStepPoints) return false
        if (inTunnel != other.inTunnel) return false
        if (distanceRemaining != other.distanceRemaining) return false
        if (distanceTraveled != other.distanceTraveled) return false
        if (durationRemaining != other.durationRemaining) return false
        if (fractionTraveled != other.fractionTraveled) return false
        if (remainingWaypoints != other.remainingWaypoints) return false
        if (upcomingRoadObjects != other.upcomingRoadObjects) return false
        if (stale != other.stale) return false
        if (routeAlternativeId != other.routeAlternativeId) return false
        if (currentRouteGeometryIndex != other.currentRouteGeometryIndex) return false
        if (alternativeRoutesIndices != other.alternativeRoutesIndices) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = navigationRoute.hashCode()
        result = 31 * result + bannerInstructions.hashCode()
        result = 31 * result + voiceInstructions.hashCode()
        result = 31 * result + currentState.hashCode()
        result = 31 * result + currentLegProgress.hashCode()
        result = 31 * result + upcomingStepPoints.hashCode()
        result = 31 * result + inTunnel.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        result = 31 * result + distanceTraveled.hashCode()
        result = 31 * result + durationRemaining.hashCode()
        result = 31 * result + fractionTraveled.hashCode()
        result = 31 * result + remainingWaypoints
        result = 31 * result + upcomingRoadObjects.hashCode()
        result = 31 * result + stale.hashCode()
        result = 31 * result + routeAlternativeId.hashCode()
        result = 31 * result + currentRouteGeometryIndex.hashCode()
        result = 31 * result + alternativeRoutesIndices.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteProgress(" +
            "navigationRoute=$navigationRoute, " +
            "currentState=$currentState, " +
            "inTunnel=$inTunnel, " +
            "distanceRemaining=$distanceRemaining, " +
            "distanceTraveled=$distanceTraveled, " +
            "durationRemaining=$durationRemaining, " +
            "fractionTraveled=$fractionTraveled, " +
            "stale=$stale, " +
            "routeAlternativeId=$routeAlternativeId, " +
            "currentRouteGeometryIndex=$currentRouteGeometryIndex, " +
            "currentLegProgress=$currentLegProgress, " +
            "bannerInstructions=$bannerInstructions, " +
            "voiceInstructions=$voiceInstructions, " +
            "upcomingStepPoints=$upcomingStepPoints, " +
            "remainingWaypoints=$remainingWaypoints, " +
            "upcomingRoadObjects=$upcomingRoadObjects" +
            "alternativeRoutesIndices=$alternativeRoutesIndices" +
            ")"
    }
}
