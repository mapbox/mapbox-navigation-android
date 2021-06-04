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
class RouteProgress private constructor(
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
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder(route)
        .bannerInstructions(bannerInstructions)
        .voiceInstructions(voiceInstructions)
        .currentState(currentState)
        .currentLegProgress(currentLegProgress)
        .upcomingStepPoints(upcomingStepPoints)
        .inTunnel(inTunnel)
        .distanceRemaining(distanceRemaining)
        .distanceTraveled(distanceTraveled)
        .durationRemaining(durationRemaining)
        .fractionTraveled(fractionTraveled)
        .remainingWaypoints(remainingWaypoints)
        .upcomingRoadObjects(upcomingRoadObjects)
        .stale(stale)

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

    /**
     * Builder for [RouteProgress]
     *
     * @param route [DirectionsRoute] currently is used for the navigation session
     */
    class Builder(private val route: DirectionsRoute) {
        private var bannerInstructions: BannerInstructions? = null
        private var voiceInstructions: VoiceInstructions? = null
        private var currentState: RouteProgressState = RouteProgressState.INITIALIZED
        private var currentLegProgress: RouteLegProgress? = null
        private var upcomingStepPoints: List<Point>? = null
        private var inTunnel: Boolean = false
        private var distanceRemaining: Float = 0f
        private var distanceTraveled: Float = 0f
        private var durationRemaining: Double = 0.0
        private var fractionTraveled: Float = 0f
        private var remainingWaypoints: Int = 0
        private var upcomingRoadObjects: List<UpcomingRoadObject> = emptyList()
        private var stale: Boolean = false

        /**
         * Current banner instruction.
         *
         * @return Builder
         */
        fun bannerInstructions(bannerInstructions: BannerInstructions?): Builder =
            apply { this.bannerInstructions = bannerInstructions }

        /**
         * Current voice instruction.
         *
         * @return Builder
         */
        fun voiceInstructions(voiceInstructions: VoiceInstructions?): Builder =
            apply { this.voiceInstructions = voiceInstructions }

        /**
         * The current state of progress along the route. Provides route and location tracking
         * information.
         *
         * @return Builder
         */
        fun currentState(currentState: RouteProgressState): Builder =
            apply { this.currentState = currentState }

        /**
         * [RouteLegProgress] object with information about the particular leg the user is
         * currently on.
         *
         * @return Builder
         */
        fun currentLegProgress(legProgress: RouteLegProgress?): Builder =
            apply { this.currentLegProgress = legProgress }

        /**
         * The list of points that represent the upcoming step geometry.
         *
         * @return Builder
         */
        fun upcomingStepPoints(upcomingStepPoints: List<Point>?): Builder =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        /**
         * *true* if in a tunnel, *false* otherwise
         *
         * @return Builder
         */
        fun inTunnel(inTunnel: Boolean): Builder = apply { this.inTunnel = inTunnel }

        /**
         * The distance remaining in meters until the user reaches the end of the route.
         *
         * @return Builder
         */
        fun distanceRemaining(distanceRemaining: Float): Builder =
            apply { this.distanceRemaining = distanceRemaining }

        /**
         * Total distance traveled in meters along the route.
         *
         * @return Builder
         */
        fun distanceTraveled(distanceTraveled: Float): Builder =
            apply { this.distanceTraveled = distanceTraveled }

        /**
         * The duration remaining in seconds until the user reaches the end of the route
         *
         * @return Builder
         */
        fun durationRemaining(durationRemaining: Double): Builder =
            apply { this.durationRemaining = durationRemaining }

        /**
         * The fraction traveled along the current route. This is a float value between 0 and 1 and
         * isn't guaranteed to reach 1 before the user reaches the end of the route.
         *
         * @return Builder
         */
        fun fractionTraveled(fractionTraveled: Float): Builder =
            apply { this.fractionTraveled = fractionTraveled }

        /**
         * Number of waypoints remaining on the current route
         *
         * @return Builder
         */
        fun remainingWaypoints(remainingWaypoints: Int): Builder =
            apply { this.remainingWaypoints = remainingWaypoints }

        /**
         * List of upcoming road objects with distances from current location to each of them.
         *
         * @return Builder
         */
        fun upcomingRoadObjects(upcomingRoadObjects: List<UpcomingRoadObject>): Builder =
            apply { this.upcomingRoadObjects = upcomingRoadObjects }

        /**
         * True if there were no location updates for a significant amount of time which causes
         * a lack of confidence in the progress updates being sent.
         *
         * @return Builder
         */
        fun stale(stale: Boolean): Builder =
            apply { this.stale = stale }

        /**
         * Build new instance of [RouteProgress]
         *
         * @return RouteProgress
         * @throws IllegalStateException if [DirectionsRoute] is not provided
         */
        fun build(): RouteProgress {
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
}
