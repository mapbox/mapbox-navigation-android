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
 *
 * @param route [DirectionsRoute] the navigation session is currently using. When a reroute occurs and a new
 * directions route gets obtained, with the next location update this directions route should
 * reflect the new route.
 *
 * @param eHorizon [ElectronicHorizon] object with Electronic Horizon information
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * For now, Electronic Horizon only works in Free Drive.
 *
 * @param routeGeometryWithBuffer [Geometry] of the current [DirectionsRoute] with a buffer
 * that encompasses visible tile surface are while navigating. This [Geometry] is ideal
 * for offline downloads of map or routing tile data.
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
 */
data class RouteProgress(
    val route: DirectionsRoute?,
    val eHorizon: ElectronicHorizon?,
    val routeGeometryWithBuffer: Geometry?,
    val bannerInstructions: BannerInstructions?,
    val voiceInstructions: VoiceInstructions?,
    val currentState: RouteProgressState?,
    val currentLegProgress: RouteLegProgress?,
    val upcomingStepPoints: List<Point>?,
    val inTunnel: Boolean,
    val distanceRemaining: Float,
    val distanceTraveled: Float,
    val durationRemaining: Double,
    val fractionTraveled: Float,
    val remainingWaypoints: Int
) {
    /**
     * Builder for [RouteProgress]
     */
    class Builder {
        private var directionsRoute: DirectionsRoute? = null
        private var electronicHorizon: ElectronicHorizon? = null
        private var routeGeometryWithBuffer: Geometry? = null
        private var bannerInstructions: BannerInstructions? = null
        private var voiceInstructions: VoiceInstructions? = null
        private var currentState: RouteProgressState? = null
        private var currentLegProgress: RouteLegProgress? = null
        private var upcomingStepPoints: List<Point>? = null
        private var inTunnel: Boolean = false
        private var distanceRemaining: Float = 0f
        private var distanceTraveled: Float = 0f
        private var durationRemaining: Double = 0.0
        private var fractionTraveled: Float = 0f
        private var remainingWaypoints: Int = 0

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
                remainingWaypoints
            )
        }
    }
}
