package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point

class RouteProgress private constructor(
    private val route: DirectionsRoute? = null,
    private val routeGeometryWithBuffer: Geometry? = null,
    private val bannerInstructions: BannerInstructions? = null,
    private val voiceInstructions: VoiceInstructions? = null,
    private val currentState: RouteProgressState? = null,
    private val currentLegProgress: RouteLegProgress? = null,
    private val upcomingStepPoints: List<Point>? = null,
    private val inTunnel: Boolean = false,
    private val distanceRemaining: Float = 0f,
    private val distanceTraveled: Float = 0f,
    private val durationRemaining: Long = 0L,
    private val fractionTraveled: Float = 0f,
    private val remainingWaypoints: Int = 0,
    private val builder: Builder
) {

    /**
     * Get the route the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route.
     *
     * @return a [DirectionsRoute] currently being used for the navigation session
     */
    fun route() = route

    /**
     * Total distance traveled in meters along route.
     *
     * @return a Float value representing the total distance the user has traveled along the route,
     * using unit meters
     */
    fun distanceTraveled(): Float = distanceTraveled

    /**
     * Provides the duration remaining in milliseconds till the user reaches the end of the route.
     *
     * @return `long` value representing the duration remaining till end of route, in milliseconds
     */
    fun durationRemaining(): Long = durationRemaining

    /**
     * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the end of the route.
     *
     * @return a Float value between 0 and 1 representing the fraction the user has traveled along the
     * route
     */
    private fun fractionTraveled(): Float? = fractionTraveled

    /**
     * Provides the distance remaining in meters till the user reaches the end of the route.
     *
     * @return `long` value representing the distance remaining till end of route, in unit meters
     */
    fun distanceRemaining(): Float = distanceRemaining

    /**
     * Number of waypoints remaining on the current route.
     *
     * @return integer value representing the number of way points remaining along the route
     */
    fun remainingWaypoints(): Int = remainingWaypoints

    /**
     * Gives a [RouteLegProgress] object with information about the particular leg the user is
     * currently on.
     *
     * @return a [RouteLegProgress] object
     */
    fun currentLegProgress() = currentLegProgress

    /**
     * Provides a list of points that represent the upcoming step
     * step geometry.
     *
     * @return list of points representing the upcoming step
     */
    fun upcomingStepPoints() = upcomingStepPoints

    /**
     * Returns whether or not the location updates are
     * considered in a tunnel along the route.
     *
     * @return true if in a tunnel, false otherwise
     */
    fun inTunnel() = inTunnel

    /**
     * Current banner instruction.
     *
     * @return current banner instruction
     */
    fun bannerInstructions() = bannerInstructions

    /**
     * Current voice instruction.
     *
     * @return current voice instruction
     */
    fun voiceInstructions() = voiceInstructions

    /**
     * Returns the current state of progress along the route.  Provides route and location tracking
     * information.
     *
     * @return the current state of progress along the route.
     */
    fun currentState() = currentState

    /**
     * Returns the current [DirectionsRoute] geometry with a buffer
     * that encompasses visible tile surface are while navigating.
     *
     *
     * This [Geometry] is ideal for offline downloads of map or routing tile
     * data.
     *
     * @return current route geometry with buffer
     */
    fun routeGeometryWithBuffer() = routeGeometryWithBuffer

    fun toBuilder() = builder

    data class Builder(
        private var directionsRoute: DirectionsRoute? = null,
        private var routeGeometryWithBuffer: Geometry? = null,
        private var bannerInstructions: BannerInstructions? = null,
        private var voiceInstructions: VoiceInstructions? = null,
        private var currentState: RouteProgressState? = null,
        private var currentLegProgress: RouteLegProgress? = null,
        private var upcomingStepPoints: List<Point>? = null,
        private var inTunnel: Boolean = false,
        private var distanceRemaining: Float = 0f,
        private var distanceTraveled: Float = 0f,
        private var durationRemaining: Long = 0L,
        private var fractionTraveled: Float = 0f,
        private var remainingWaypoints: Int = 0
    ) {

        fun route(route: DirectionsRoute) =
            apply { this.directionsRoute = route }

        fun routeGeometryWithBuffer(routeGeometryWithBuffer: Geometry?) =
            apply { this.routeGeometryWithBuffer = routeGeometryWithBuffer }

        fun bannerInstructions(bannerInstructions: BannerInstructions?) =
            apply { this.bannerInstructions = bannerInstructions }

        fun voiceInstructions(voiceInstructions: VoiceInstructions?) =
            apply { this.voiceInstructions = voiceInstructions }

        fun currentState(currentState: RouteProgressState) =
            apply { this.currentState = currentState }

        fun currentLegProgress(legProgress: RouteLegProgress) =
            apply { this.currentLegProgress = legProgress }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }

        fun distanceRemaining(distanceRemaining: Float) =
            apply { this.distanceRemaining = distanceRemaining }

        fun distanceTraveled(distanceTraveled: Float) =
            apply { this.distanceTraveled = distanceTraveled }

        fun durationRemaining(durationRemaining: Long) =
            apply { this.durationRemaining = durationRemaining }

        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        fun remainingWaypoints(remainingWaypoints: Int) =
            apply { this.remainingWaypoints = remainingWaypoints }

        fun build(): RouteProgress {
            return RouteProgress(
                directionsRoute,
                routeGeometryWithBuffer,
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
                this
            )
        }
    }
}
