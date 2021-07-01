package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject

/**
 * This class contains all progress information at any given time during a navigation session. This
 * progress includes information for the current route, leg, and step that the user is traveling along.
 * With every new valid location update, a new route progress will be generated using the latest
 * information.
 *
 * The latest route progress object can be obtained through the [com.mapbox.navigation.core.trip.session.RouteProgressObserver].
 * Note that the route progress object's immutable.
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
class RouteProgress internal constructor(
    val route: DirectionsRoute,
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
    val stale: Boolean
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteProgress

        if (route != other.route) return false
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

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + (bannerInstructions?.hashCode() ?: 0)
        result = 31 * result + (voiceInstructions?.hashCode() ?: 0)
        result = 31 * result + currentState.hashCode()
        result = 31 * result + (currentLegProgress?.hashCode() ?: 0)
        result = 31 * result + (upcomingStepPoints?.hashCode() ?: 0)
        result = 31 * result + inTunnel.hashCode()
        result = 31 * result + distanceRemaining.hashCode()
        result = 31 * result + distanceTraveled.hashCode()
        result = 31 * result + durationRemaining.hashCode()
        result = 31 * result + fractionTraveled.hashCode()
        result = 31 * result + remainingWaypoints
        result = 31 * result + upcomingRoadObjects.hashCode()
        result = 31 * result + stale.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteProgress(" +
            "route=$route, " +
            "bannerInstructions=$bannerInstructions, " +
            "voiceInstructions=$voiceInstructions, " +
            "currentState=$currentState, " +
            "currentLegProgress=$currentLegProgress, " +
            "upcomingStepPoints=$upcomingStepPoints, " +
            "inTunnel=$inTunnel, " +
            "distanceRemaining=$distanceRemaining, " +
            "distanceTraveled=$distanceTraveled, " +
            "durationRemaining=$durationRemaining, " +
            "fractionTraveled=$fractionTraveled, " +
            "remainingWaypoints=$remainingWaypoints, " +
            "upcomingRoadObjects=$upcomingRoadObjects, " +
            "stale=$stale" +
            ")"
    }
}
