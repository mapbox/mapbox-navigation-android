package com.mapbox.navigation.base.trip.model

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point

/**
 * This class contains all progress information at any given time during a navigation session. This
 * progress includes information for the current route, leg, and step that the user is traveling along.
 * With every new valid location update, a new route progress will be generated using the latest
 * information.
 *
 * The latest route progress object can be obtained through the [com.mapbox.navigation.core.trip.session.RouteProgressObserver].
 * Note that the route progress object's immutable.
 */
class RouteProgress private constructor(
    private val route: DirectionsRoute? = null,
    private val eHorizon: ElectronicHorizon? = null,
    private val routeGeometryWithBuffer: Geometry? = null,
    private val bannerInstructions: BannerInstructions? = null,
    private val voiceInstructions: VoiceInstructions? = null,
    private val currentState: RouteProgressState? = null,
    private val currentLegProgress: RouteLegProgress? = null,
    private val upcomingStepPoints: List<Point>? = null,
    private val inTunnel: Boolean = false,
    private val distanceRemaining: Float = 0f,
    private val distanceTraveled: Float = 0f,
    private val durationRemaining: Double = 0.0,
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
     * Provides an [ElectronicHorizon].
     *
     * Electronic Horizon is still **experimental**, which means that the design of the
     * APIs has open issues which may (or may not) lead to their changes in the future.
     * Roughly speaking, there is a chance that those declarations will be deprecated in the near
     * future or the semantics of their behavior may change in some way that may break some code.
     *
     * For now, Electronic Horizon only works in Free Drive.
     *
     * @return a [ElectronicHorizon] object
     */
    fun eHorizon(): ElectronicHorizon? = eHorizon

    /**
     * Total distance traveled in meters along route.
     *
     * @return a Float value representing the total distance the user has traveled along the route,
     * using unit meters
     */
    fun distanceTraveled(): Float = distanceTraveled

    /**
     * Provides the duration remaining in seconds until the user reaches the end of the route.
     *
     * @return Double value representing the duration remaining till end of route, in seconds
     */
    fun durationRemaining(): Double = durationRemaining

    /**
     * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the end of the route.
     *
     * @return a Float value between 0 and 1 representing the fraction the user has traveled along the
     * route
     */
    private fun fractionTraveled(): Float? = fractionTraveled

    /**
     * Provides the distance remaining in meters until the user reaches the end of the route.
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
     * @return *true* if in a tunnel, *false* otherwise
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
     * This [Geometry] is ideal for offline downloads of map or routing tile
     * data.
     *
     * @return current route geometry with buffer
     */
    fun routeGeometryWithBuffer() = routeGeometryWithBuffer

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = builder

    /**
     * Builder for [RouteProgress]
     *
     * @param directionsRoute [DirectionsRoute] currently is used for the navigation session
     * @param electronicHorizon [ElectronicHorizon] object with Electronic Horizon information
     * @param routeGeometryWithBuffer Current [DirectionsRoute] geometry with a buffer
     * that encompasses visible tile surface are while navigating.
     *
     * This [Geometry] is ideal for offline downloads of map or routing tile
     * data.
     *
     * @param bannerInstructions Current banner instruction.
     * @param voiceInstructions Current voice instruction.
     * @param currentState The current state of progress along the route. Provides route and location
     * tracking information.
     * @param currentLegProgress [RouteLegProgress] object with information about the particular
     * leg the user is currently on.
     * @param upcomingStepPoints The list of points that represent the upcoming step geometry.
     * @param inTunnel *true* if in a tunnel, *false* otherwise
     * @param distanceRemaining The distance remaining in meters until the user reaches the end of the route
     * @param distanceTraveled Total distance traveled in meters along route
     * @param durationRemaining The duration remaining in seconds until the user reaches the end of the route
     * @param fractionTraveled Float
     * @param remainingWaypoints Number of waypoints remaining on the current route
     */
    data class Builder(
        private var directionsRoute: DirectionsRoute? = null,
        private var electronicHorizon: ElectronicHorizon? = null,
        private var routeGeometryWithBuffer: Geometry? = null,
        private var bannerInstructions: BannerInstructions? = null,
        private var voiceInstructions: VoiceInstructions? = null,
        private var currentState: RouteProgressState? = null,
        private var currentLegProgress: RouteLegProgress? = null,
        private var upcomingStepPoints: List<Point>? = null,
        private var inTunnel: Boolean = false,
        private var distanceRemaining: Float = 0f,
        private var distanceTraveled: Float = 0f,
        private var durationRemaining: Double = 0.0,
        private var fractionTraveled: Float = 0f,
        private var remainingWaypoints: Int = 0
    ) {

        /**
         * [DirectionsRoute] currently is used for the navigation session
         *
         * @return Builder
         */
        fun route(route: DirectionsRoute) =
            apply { this.directionsRoute = route }

        /**
         * [ElectronicHorizon] object with Electronic Horizon information
         *
         * @return Builder
         */
        fun eHorizon(eHorizon: ElectronicHorizon) =
            apply { this.electronicHorizon = eHorizon }

        /**
         * Current [DirectionsRoute] geometry with a buffer
         * that encompasses visible tile surface are while navigating.
         *
         * This [Geometry] is ideal for offline downloads of map or routing tile
         * data.
         *
         * @return Builder
         */
        fun routeGeometryWithBuffer(routeGeometryWithBuffer: Geometry?) =
            apply { this.routeGeometryWithBuffer = routeGeometryWithBuffer }

        /**
         * Current banner instruction.
         *
         * @return Builder
         */
        fun bannerInstructions(bannerInstructions: BannerInstructions?) =
            apply { this.bannerInstructions = bannerInstructions }

        /**
         * Current voice instruction.
         *
         * @return Builder
         */
        fun voiceInstructions(voiceInstructions: VoiceInstructions?) =
            apply { this.voiceInstructions = voiceInstructions }

        /**
         * The current state of progress along the route.  Provides route and location tracking
         * information.
         *
         * @return Builder
         */
        fun currentState(currentState: RouteProgressState) =
            apply { this.currentState = currentState }

        /**
         * [RouteLegProgress] object with information about the particular leg the user is
         * currently on.
         *
         * @return Builder
         */
        fun currentLegProgress(legProgress: RouteLegProgress) =
            apply { this.currentLegProgress = legProgress }

        /**
         * The list of points that represent the upcoming step geometry.
         *
         * @return Builder
         */
        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        /**
         * *true* if in a tunnel, *false* otherwise
         *
         * @return Builder
         */
        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }

        /**
         * The distance remaining in meters until the user reaches the end of the route.
         *
         * @return Builder
         */
        fun distanceRemaining(distanceRemaining: Float) =
            apply { this.distanceRemaining = distanceRemaining }

        /**
         * Total distance traveled in meters along the route.
         *
         * @return Builder
         */
        fun distanceTraveled(distanceTraveled: Float) =
            apply { this.distanceTraveled = distanceTraveled }

        /**
         * The duration remaining in seconds until the user reaches the end of the route
         *
         * @return Builder
         */
        fun durationRemaining(durationRemaining: Double) =
            apply { this.durationRemaining = durationRemaining }

        /**
         * The fraction traveled along the current route. This is a float value between 0 and 1 and
         * isn't guaranteed to reach 1 before the user reaches the end of the route.
         *
         * @return Builder
         */
        fun fractionTraveled(fractionTraveled: Float) =
            apply { this.fractionTraveled = fractionTraveled }

        /**
         * Number of waypoints remaining on the current route
         *
         * @return Builder
         */
        fun remainingWaypoints(remainingWaypoints: Int) =
            apply { this.remainingWaypoints = remainingWaypoints }

        /**
         * Build new instance of [RouteProgress]
         *
         * @return RouteProgress
         */
        fun build(): RouteProgress {
            return RouteProgress(
                directionsRoute,
                electronicHorizon,
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
