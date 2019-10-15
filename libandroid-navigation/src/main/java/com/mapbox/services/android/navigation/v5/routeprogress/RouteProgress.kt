package com.mapbox.services.android.navigation.v5.routeprogress

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.VoiceInstruction
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.v5.utils.extensions.plus

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
 * @since 0.1.0
 *
 * @property directionsRoute DirectionsRoute - Get the route the navigation session is currently using.
 * When a reroute occurs and a new directions route gets obtained, with the next location update this
 * directions route should reflect the new route. All direction route get passed in through
 * [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation.startNavigation].
 * @since 0.1.0
 *
 * @property legIndex Int - Index representing the current leg the user is on. If the directions route currently in use
 * contains more then two waypoints, the route is likely to have multiple legs representing the
 * distance between the two points.
 * @since 0.1.0
 *
 * @property distanceRemaining Double - Provides the distance remaining in meters till the user reaches the end of the route.
 * Unit is meter.
 * @since 0.1.0
 *
 * @property currentLegProgress RouteLegProgress - Gives a [RouteLegProgress] object with information about
 * the particular leg the user is currently on.
 * @since 0.1.0
 *
 * @property currentStepPoints List<Point> - Provides a list of points that represent the current step
 * step geometry.
 * @since 0.12.0
 *
 * @property upcomingStepPoints List<Point>? - Provides a list of points that represent the upcoming step
 * step geometry.
 * @since 0.12.0
 *
 * @property inTunnel Boolean - Returns whether or not the location updates are
 * considered in a tunnel along the route.
 * @since 0.19.0
 *
 * @property voiceInstruction VoiceInstruction? - Current voice instruction.
 * @since 0.20.0
 *
 * @property bannerInstruction BannerInstruction? - Current banner instruction.
 * @since 0.25.0
 *
 * @property currentState RouteProgressState? - Returns the current state of progress along the route.
 * Provides route and location tracking information.
 *
 * @property routeGeometryWithBuffer Geometry? - Returns the current [DirectionsRoute] geometry with a buffer
 * that encompasses visible tile surface are while navigating.
 * This [Geometry] is ideal for offline downloads of map or routing tile data.
 *
 * @property currentStep LegStep
 * @property stepIndex Int
 * @property legDistanceRemaining Double
 * @property stepDistanceRemaining Double
 * @property legDurationRemaining Double
 * @constructor
 */
data class RouteProgress
@JvmOverloads
internal constructor(
    val directionsRoute: DirectionsRoute,
    val legIndex: Int,
    val distanceRemaining: Double,
    val currentLegProgress: RouteLegProgress,
    val currentStepPoints: List<Point>,
    val upcomingStepPoints: List<Point>? = null,
    val inTunnel: Boolean,
    val voiceInstruction: VoiceInstruction? = null,
    val bannerInstruction: BannerInstruction? = null,
    val currentState: RouteProgressState? = null,
    val routeGeometryWithBuffer: Geometry? = null,
    val currentStep: LegStep,
    val stepIndex: Int,
    val legDistanceRemaining: Double,
    val stepDistanceRemaining: Double,
    val legDurationRemaining: Double
) {

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    // /**
    //  * Get the route the navigation session is currently using. When a reroute occurs and a new
    //  * directions route gets obtained, with the next location update this directions route should
    //  * reflect the new route. All direction route get passed in through
    //  * [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation.startNavigation].
    //  *
    //  * @return a [DirectionsRoute] currently being used for the navigation session
    //  * @since 0.1.0
    //  */
    // fun directionsRoute() = directionsRoute
    //
    // /**
    //  * Index representing the current leg the user is on. If the directions route currently in use
    //  * contains more then two waypoints, the route is likely to have multiple legs representing the
    //  * distance between the two points.
    //  *
    //  * @return an integer representing the current leg the user is on
    //  * @since 0.1.0
    //  */
    // fun legIndex(): Int? = legIndex

    /**
     * Provides the current [RouteLeg] the user is on.
     *
     * @return a [RouteLeg] the user is currently on
     * @since 0.1.0
     */
    fun currentLeg(): RouteLeg? = ifNonNull(directionsRoute.legs(), legIndex) { legs, legIndex ->
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
        ifNonNull(directionsRoute.distance(), distanceRemaining) { distance, distanceRemaining ->
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
        ifNonNull(fractionTraveled(), directionsRoute.duration()) { fractionTraveled, duration ->
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
        ifNonNull(distanceTraveled(), directionsRoute.distance()) { distanceTraveled, distance ->
            when (distance > 0) {
                true -> (distanceTraveled / distance).toFloat()
                false -> 1.0f
            }
        } ?: 1.0f

    // /**
    //  * Provides the distance remaining in meters till the user reaches the end of the route.
    //  *
    //  * @return `long` value representing the distance remaining till end of route, in unit meters
    //  * @since 0.1.0
    //  */
    // fun distanceRemaining(): Double? = distanceRemaining

    /**
     * Number of waypoints remaining on the current route.
     *
     * @return integer value representing the number of way points remaining along the route
     * @since 0.5.0
     */
    fun remainingWaypoints(): Int? =
        ifNonNull(directionsRoute.legs(), legIndex) { legs, legIndex -> legs.size - legIndex }

    // /**
    //  * Gives a [RouteLegProgress] object with information about the particular leg the user is
    //  * currently on.
    //  *
    //  * @return a [RouteLegProgress] object
    //  * @since 0.1.0
    //  */
    // fun currentLegProgress() = currentLegProgress

    // /**
    //  * Provides a list of points that represent the current step
    //  * step geometry.
    //  *
    //  * @return list of points representing the current step
    //  * @since 0.12.0
    //  */
    // fun currentStepPoints() = currentStepPoints

    // /**
    //  * Provides a list of points that represent the upcoming step
    //  * step geometry.
    //  *
    //  * @return list of points representing the upcoming step
    //  * @since 0.12.0
    //  */
    // fun upcomingStepPoints() = upcomingStepPoints

    // /**
    //  * Returns whether or not the location updates are
    //  * considered in a tunnel along the route.
    //  *
    //  * @return true if in a tunnel, false otherwise
    //  * @since 0.19.0
    //  */
    // fun inTunnel() = inTunnel

    // /**
    //  * Current voice instruction.
    //  *
    //  * @return current voice instruction
    //  * @since 0.20.0
    //  */
    // fun voiceInstruction() = voiceInstruction

    // /**
    //  * Current banner instruction.
    //  *
    //  * @return current banner instruction
    //  * @since 0.25.0
    //  */
    // fun bannerInstruction() = bannerInstruction

    // /**
    //  * Returns the current state of progress along the route.  Provides route and location tracking
    //  * information.
    //  *
    //  * @return the current state of progress along the route.
    //  */
    // fun currentState() = currentState

    // /**
    //  * Returns the current [DirectionsRoute] geometry with a buffer
    //  * that encompasses visible tile surface are while navigating.
    //  *
    //  *
    //  * This [Geometry] is ideal for offline downloads of map or routing tile
    //  * data.
    //  *
    //  * @return current route geometry with buffer
    //  */
    // fun routeGeometryWithBuffer() = routeGeometryWithBuffer

    fun toBuilder() = builder()
        .directionsRoute(directionsRoute)
        .legIndex(legIndex)
        .distanceRemaining(distanceRemaining)
        .currentLegProgress(currentLegProgress)
        .currentStepPoints(currentStepPoints)
        .upcomingStepPoints(upcomingStepPoints)
        .inTunnel(inTunnel)
        .voiceInstruction(voiceInstruction)
        .bannerInstruction(bannerInstruction)
        .currentState(currentState)
        .routeGeometryWithBuffer(routeGeometryWithBuffer)
        .currentStep(currentStep)
        .stepIndex(stepIndex)
        .legDistanceRemaining(legDistanceRemaining)
        .stepDistanceRemaining(stepDistanceRemaining)
        .legDurationRemaining(legDurationRemaining)

    class Builder internal constructor() {
        private var directionsRoute: DirectionsRoute? = null
        private var legIndex: Int? = null
        private var distanceRemaining: Double? = null
        private var currentLegProgress: RouteLegProgress? = null
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var inTunnel: Boolean? = null
        private var voiceInstruction: VoiceInstruction? = null
        private var bannerInstruction: BannerInstruction? = null
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

        fun currentStepPoints(currentStepPoints: List<Point>) =
            apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }
        fun voiceInstruction(voiceInstruction: VoiceInstruction?) =
            apply { this.voiceInstruction = voiceInstruction }

        fun bannerInstruction(bannerInstruction: BannerInstruction?) =
            apply { this.bannerInstruction = bannerInstruction }

        fun currentState(currentState: RouteProgressState?) =
            apply { this.currentState = currentState }

        fun routeGeometryWithBuffer(routeGeometryWithBuffer: Geometry?) =
            apply { this.routeGeometryWithBuffer = routeGeometryWithBuffer }

        fun currentStep(currentStep: LegStep) = apply { this.currentStep = currentStep }
        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }
        fun legDistanceRemaining(legDistanceRemaining: Double) =
            apply { this.legDistanceRemaining = legDistanceRemaining }

        fun stepDistanceRemaining(stepDistanceRemaining: Double) =
            apply { this.stepDistanceRemaining = stepDistanceRemaining }

        fun legDurationRemaining(legDurationRemaining: Double) =
            apply { this.legDurationRemaining = legDurationRemaining }

        private fun validate() {
            val missing = StringBuilder()
            if (this.directionsRoute == null) {
                missing + " directionsRoute"
            }
            if (this.legIndex == null) {
                missing + " legIndex"
            }
            if (this.distanceRemaining == null) {
                missing + " distanceRemaining"
            }
            if (this.currentLegProgress == null) {
                missing + " currentLegProgress"
            }
            if (this.currentStepPoints == null) {
                missing + " currentStepPoints"
            }
            if (this.inTunnel == null) {
                missing + " inTunnel"
            }
            if (this.currentStep == null) {
                missing + " currentStep"
            }
            if (this.stepIndex == null) {
                missing + " stepIndex"
            }
            if (this.legDistanceRemaining == null) {
                missing + " legDistanceRemaining"
            }
            if (this.stepDistanceRemaining == null) {
                missing + " stepDistanceRemaining"
            }
            if (this.legDurationRemaining == null) {
                missing + " legDurationRemaining"
            }
            check(missing.isEmpty()) { "Missing required properties:$missing" }
        }

        fun build(): RouteProgress {
            return ifNonNull(
                directionsRoute,
                legDistanceRemaining,
                distanceRemaining,
                stepIndex,
                legDurationRemaining,
                stepDistanceRemaining,
                legIndex,
                inTunnel,
                currentStepPoints,
                currentStep
            ) { _directionsRoute, _legDistanceRemaining,
                _distanceRemaining, _stepIndex,
                _legDurationRemaining, _stepDistanceRemaining,
                _legIndex, _inTunnel,
                 _currentStepPoints, _currentStep ->

                val leg: RouteLeg? = _directionsRoute.let { directionRoute ->
                    directionRoute.legs()?.let { legs ->
                        legs[_legIndex]
                    }
                }
                val legProgress = RouteLegProgress.builder()
                    .routeLeg(leg)
                    .currentStep(_currentStep)
                    .stepIndex(_stepIndex)
                    .distanceRemaining(_legDistanceRemaining)
                    .durationRemaining(_legDurationRemaining)
                    .stepDistanceRemaining(_stepDistanceRemaining)
                    .currentStepPoints(_currentStepPoints)
                    .upcomingStepPoints(upcomingStepPoints)
                    .build()
                currentLegProgress(legProgress)
                validate()

                return RouteProgress(
                    directionsRoute = _directionsRoute,
                    legIndex = _legIndex,
                    distanceRemaining = _distanceRemaining,
                    currentLegProgress = legProgress,
                    currentStepPoints = _currentStepPoints,
                    upcomingStepPoints = upcomingStepPoints,
                    inTunnel = _inTunnel,
                    voiceInstruction = voiceInstruction,
                    bannerInstruction = bannerInstruction,
                    currentState = currentState,
                    routeGeometryWithBuffer = routeGeometryWithBuffer,
                    currentStep = _currentStep,
                    stepIndex = _stepIndex,
                    legDistanceRemaining = _legDistanceRemaining,
                    stepDistanceRemaining = _stepDistanceRemaining,
                    legDurationRemaining = _legDurationRemaining
                )
            } ?: throw IllegalStateException("Missing required properties")
        }
    }
}
