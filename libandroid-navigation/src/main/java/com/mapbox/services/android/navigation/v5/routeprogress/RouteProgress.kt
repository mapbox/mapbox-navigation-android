package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * This class contains all progress information at any given time during a navigation session. This
 * progress includes information for the current route, leg and step the user is traversing along.
 * With every new valid location update, a new route progress will be generated using the latest
 * information.
 *
 *
 * The latest route progress object can be obtained through either the [ProgressChangeListener]
 * or the [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener] callbacks.
 * Note that the route progress object's immutable.
 *
 *
 * @since 0.1.0
 */
data class RouteProgress internal constructor(
    var directionsRoute: DirectionsRoute? = null,
    var legIndex: Int? = null,
    var distanceRemaining: Double? = null,
    var currentLegProgress: RouteLegProgress? = null,
    var currentStepPoints: List<Point>? = null,
    var upcomingStepPoints: List<Point>? = null,
    var inTunnel: Boolean? = null,
    var voiceInstruction: VoiceInstructions? = null,
    var bannerInstruction: BannerInstructions? = null,
    var currentState: RouteProgressState? = null,
    var routeGeometryWithBuffer: Geometry? = null,
    var currentStep: LegStep? = null,
    var stepIndex: Int? = null,
    var legDistanceRemaining: Double? = null,
    var stepDistanceRemaining: Double? = null,
    var legDurationRemaining: Double? = null,
    var builder: Builder
) {

    /**
     * Get the route the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route. All direction route get passed in through
     * [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation.startNavigation].
     *
     * @return a [DirectionsRoute] currently being used for the navigation session
     * @since 0.1.0
     */
    fun directionsRoute() = directionsRoute

    /**
     * Index representing the current leg the user is on. If the directions route currently in use
     * contains more then two waypoints, the route is likely to have multiple legs representing the
     * distance between the two points.
     *
     * @return an integer representing the current leg the user is on
     * @since 0.1.0
     */
    fun legIndex(): Int? = legIndex

    /**
     * Provides the current [RouteLeg] the user is on.
     *
     * @return a [RouteLeg] the user is currently on
     * @since 0.1.0
     */
    fun currentLeg(): RouteLeg? = ifNonNull(directionsRoute()?.legs(), legIndex) { legs, legIndex ->
        legs[legIndex]
    }

    /**
     * Total distance traveled in meters along route.
     *
     * @return a double value representing the total distance the user has traveled along the route,
     * using unit meters
     * @since 0.1.0
     */
    fun distanceTraveled(): Double? =
        ifNonNull(directionsRoute?.distance(), distanceRemaining) { distance, distanceRemaining ->
            when (distance - distanceRemaining < 0) {
                true -> {
                    0.0
                }
                false -> {
                    distance - distanceRemaining
                }
            }
        }

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return `long` value representing the duration remaining till end of route, in unit
     * seconds
     * @since 0.1.0
     */
    fun durationRemaining(): Double? =
        ifNonNull(fractionTraveled(), directionsRoute()?.duration()) { fractionTraveled, duration ->
            (1 - fractionTraveled) * duration
        }

    /**
     * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the end of the route.
     *
     * @return a float value between 0 and 1 representing the fraction the user has traveled along the
     * route
     * @since 0.1.0
     */
    fun fractionTraveled(): Float? =
        ifNonNull(distanceTraveled(), directionsRoute()?.distance()) { distanceTraveled, distance ->
            when (distance > 0) {
                true -> {
                    (distanceTraveled / distance).toFloat()
                }
                false -> {
                    1.0f
                }
            }
        } ?: 1.0f

    /**
     * Provides the distance remaining in meters till the user reaches the end of the route.
     *
     * @return `long` value representing the distance remaining till end of route, in unit meters
     * @since 0.1.0
     */
    fun distanceRemaining(): Double? = distanceRemaining

    /**
     * Number of waypoints remaining on the current route.
     *
     * @return integer value representing the number of way points remaining along the route
     * @since 0.5.0
     */
    fun remainingWaypoints(): Int? =
        ifNonNull(directionsRoute?.legs(), legIndex) { legs, legIndex -> legs.size - legIndex }

    /**
     * Gives a [RouteLegProgress] object with information about the particular leg the user is
     * currently on.
     *
     * @return a [RouteLegProgress] object
     * @since 0.1.0
     */
    fun currentLegProgress() = currentLegProgress

    /**
     * Provides a list of points that represent the current step
     * step geometry.
     *
     * @return list of points representing the current step
     * @since 0.12.0
     */
    fun currentStepPoints() = currentStepPoints

    /**
     * Provides a list of points that represent the upcoming step
     * step geometry.
     *
     * @return list of points representing the upcoming step
     * @since 0.12.0
     */
    fun upcomingStepPoints() = upcomingStepPoints

    /**
     * Returns whether or not the location updates are
     * considered in a tunnel along the route.
     *
     * @return true if in a tunnel, false otherwise
     * @since 0.19.0
     */
    fun inTunnel() = inTunnel

    /**
     * Current voice instruction.
     *
     * @return current voice instruction
     * @since 0.20.0
     */
    fun voiceInstruction() = voiceInstruction

    /**
     * Current banner instruction.
     *
     * @return current banner instruction
     * @since 0.25.0
     */
    fun bannerInstruction() = bannerInstruction

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

    internal fun currentStep() = currentStep

    internal fun stepIndex() = stepIndex

    internal fun legDistanceRemaining() = legDistanceRemaining

    internal fun stepDistanceRemaining() = stepDistanceRemaining

    internal fun legDurationRemaining() = legDurationRemaining
    class Builder {
        private var directionsRoute: DirectionsRoute? = null
        private var legIndex: Int? = null
        private var distanceRemaining: Double? = null
        private var currentLegProgress: RouteLegProgress? = null
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var inTunnel: Boolean? = null
        private var voiceInstruction: VoiceInstructions? = null
        private var bannerInstruction: BannerInstructions? = null
        private var currentState: RouteProgressState? = null
        private var routeGeometryWithBuffer: Geometry? = null
        private var currentStep: LegStep? = null
        private var stepIndex: Int? = null
        private var legDistanceRemaining: Double? = null
        private var stepDistanceRemaining: Double? = null
        private var legDurationRemaining: Double? = null

        fun directionsRoute(directionsRoute: DirectionsRoute) =
            apply { this.directionsRoute = directionsRoute }

        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }
        fun distanceRemaining(distanceRemaining: Double) =
            apply { this.distanceRemaining = distanceRemaining }

        fun currentLegProgress(currentLegProgress: RouteLegProgress) =
            apply { this.currentLegProgress = currentLegProgress }

        fun currentStepPoints(currentStepPoints: List<Point>?) =
            apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }
        fun voiceInstruction(voiceInstruction: VoiceInstructions?) =
            apply { this.voiceInstruction = voiceInstruction }

        fun bannerInstruction(bannerInstruction: BannerInstructions?) =
            apply { this.bannerInstruction = bannerInstruction }

        fun currentState(currentState: RouteProgressState?) =
            apply { this.currentState = currentState }

        fun routeGeometryWithBuffer(routeGeometryWithBuffer: Geometry?) =
            apply { this.routeGeometryWithBuffer = routeGeometryWithBuffer }

        fun currentStep(currentStep: LegStep?) = apply { this.currentStep = currentStep }
        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }
        fun legDistanceRemaining(legDistanceRemaining: Double) =
            apply { this.legDistanceRemaining = legDistanceRemaining }

        fun stepDistanceRemaining(stepDistanceRemaining: Double) =
            apply { this.stepDistanceRemaining = stepDistanceRemaining }

        fun legDurationRemaining(legDurationRemaining: Double) =
            apply { this.legDurationRemaining = legDurationRemaining }

        private fun validate() {
            var missing = ""
            if (this.directionsRoute == null) {
                missing += " directionsRoute"
            }
            if (this.legIndex == null) {
                missing += " legIndex"
            }
            if (this.distanceRemaining == null) {
                missing += " distanceRemaining"
            }
            if (this.currentLegProgress == null) {
                missing += " currentLegProgress"
            }
            if (this.currentStepPoints == null) {
                missing += " currentStepPoints"
            }
            if (this.inTunnel == null) {
                missing += " inTunnel"
            }
            if (this.currentStep == null) {
                missing += " currentStep"
            }
            if (this.stepIndex == null) {
                missing += " stepIndex"
            }
            if (this.legDistanceRemaining == null) {
                missing += " legDistanceRemaining"
            }
            if (this.stepDistanceRemaining == null) {
                missing += " stepDistanceRemaining"
            }
            if (this.legDurationRemaining == null) {
                missing += " legDurationRemaining"
            }
            check(missing.isEmpty()) { "Missing required properties: $missing" }
        }

        fun build(): RouteProgress {
            // Default values for variables that are null if not initialized
            val _legDistanceRemaining = legDistanceRemaining ?: 0.0
            val _stepIndex = stepIndex ?: 0
            val _legDurationRemaining = legDurationRemaining ?: 0.0
            val _stepDistanceRemaining = stepDistanceRemaining ?: 0.0
            val _legIndex = legIndex ?: 0

            val leg: RouteLeg? = directionsRoute?.let { directionRoute ->
                directionRoute.legs()?.let { legs ->
                    legs[_legIndex]
                }
            }
            val routeLegProgressBuilder = RouteLegProgress.Builder()
            ifNonNull(leg) {
                routeLegProgressBuilder.routeLeg(it)
            }
            ifNonNull(currentStep) {
                routeLegProgressBuilder.currentStep(it)
            }
            val legProgress = routeLegProgressBuilder
                .stepIndex(_stepIndex)
                .distanceRemaining(_legDistanceRemaining)
                .durationRemaining(_legDurationRemaining)
                .stepDistanceRemaining(_stepDistanceRemaining)
                .currentStepPoints(currentStepPoints)
                .upcomingStepPoints(upcomingStepPoints)
                .build()
            currentLegProgress(legProgress)
            validate()

            return RouteProgress(
                directionsRoute,
                legIndex,
                distanceRemaining,
                currentLegProgress,
                currentStepPoints,
                upcomingStepPoints,
                inTunnel,
                voiceInstruction,
                bannerInstruction,
                currentState,
                routeGeometryWithBuffer,
                currentStep,
                stepIndex,
                legDistanceRemaining,
                stepDistanceRemaining,
                legDurationRemaining,
                this
            )
        }
    }
}
