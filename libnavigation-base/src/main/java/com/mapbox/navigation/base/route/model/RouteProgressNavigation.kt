package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.banner.BannerInstruction
import com.mapbox.navigation.base.utils.ifNonNull

class RouteProgressNavigation private constructor(
    private val route: Route? = null,
    private val currentStep: LegStepNavigation? = null,
    private val currentStepPoints: List<Point>? = null,
    private val upcomingStepPoints: List<Point>? = null,
    private val routeGeometryWithBuffer: Geometry? = null,
    private val bannerInstruction: BannerInstruction? = null,
    private val currentState: RouteProgressStateNavigation? = null,
    private val currentLegProgress: RouteLegProgressNavigation? = null,
    private val legIndex: Int = 0,
    private val stepIndex: Int = 0,
    private val inTunnel: Boolean = false,
    private val distanceRemaining: Double = 0.0,
    private val legDistanceRemaining: Double = 0.0,
    private val legDurationRemaining: Double = 0.0,
    private val stepDistanceRemaining: Double = 0.0,
    private val builder: Builder
) {

    /**
     * Get the route the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route.
     *
     * @return a [Route] currently being used for the navigation session
     */
    fun route() = route

    /**
     * Index representing the current leg the user is on. If the directions route currently in use
     * contains more then two waypoints, the route is likely to have multiple legs representing the
     * distance between the two points.
     *
     * @return an integer representing the current leg the user is on
     */
    fun legIndex(): Int = legIndex

    /**
     * Provides the current [RouteLegNavigation] the user is on.
     *
     * @return a [RouteLegNavigation] the user is currently on
     */
    fun currentLeg(): RouteLegNavigation? =
        ifNonNull(route()?.legs, legIndex) { routeLegs, legIndex ->
            routeLegs[legIndex]
        }

    /**
     * Total distance traveled in meters along route.
     *
     * @return a double value representing the total distance the user has traveled along the route,
     * using unit meters
     */
    fun distanceTraveled(): Double =
        ifNonNull(route?.distance) { distance ->
            when (distance - distanceRemaining < 0) {
                true -> {
                    0.0
                }
                false -> {
                    distance - distanceRemaining
                }
            }
        } ?: 0.0

    /**
     * Provides the duration remaining in seconds till the user reaches the end of the route.
     *
     * @return `long` value representing the duration remaining till end of route, in unit
     * seconds
     */
    fun durationRemaining(): Long =
        ifNonNull(
            fractionTraveled(),
            route()?.duration
        ) { fractionTraveled, duration ->
            ((1 - fractionTraveled) * duration).toLong()
        } ?: 0L

    /**
     * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the end of the route.
     *
     * @return a double value between 0 and 1 representing the fraction the user has traveled along the
     * route
     */
    private fun fractionTraveled(): Double? {
        if (distanceTraveled() == 0.0) {
            return 1.0
        }
        return route()?.distance?.let { distance ->
            when (distance > 0) {
                true -> {
                    (distanceTraveled() / distance)
                }
                false -> {
                    1.0
                }
            }
        } ?: 1.0
    }

    /**
     * Provides the distance remaining in meters till the user reaches the end of the route.
     *
     * @return `long` value representing the distance remaining till end of route, in unit meters
     */
    fun distanceRemaining(): Double = distanceRemaining

    /**
     * Number of waypoints remaining on the current route.
     *
     * @return integer value representing the number of way points remaining along the route
     */
    fun remainingWaypoints(): Int =
        ifNonNull(
            route?.legs,
            legIndex
        ) { legs, legIndex -> legs.size - legIndex } ?: 0

    /**
     * Gives a [RouteLegProgressNavigation] object with information about the particular leg the user is
     * currently on.
     *
     * @return a [RouteLegProgressNavigation] object
     */
    fun currentLegProgress() = currentLegProgress

    /**
     * Provides a list of points that represent the current step
     * step geometry.
     *
     * @return list of points representing the current step
     */
    fun currentStepPoints() = currentStepPoints

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
    fun bannerInstruction() = bannerInstruction

    /**
     * Returns the current state of progress along the route.  Provides route and location tracking
     * information.
     *
     * @return the current state of progress along the route.
     */
    fun currentState() = currentState

    /**
     * Returns the current [Route] geometry with a buffer
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
        private var directionsRoute: Route? = null
        private var currentStep: LegStepNavigation? = null
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var routeGeometryWithBuffer: Geometry? = null
        private var bannerInstruction: BannerInstruction? = null
        private lateinit var _currentState: RouteProgressStateNavigation
        private lateinit var _currentLegProgress: RouteLegProgressNavigation
        private var legIndex: Int = 0
        private var stepIndex: Int = 0
        private var inTunnel: Boolean = false
        private var distanceRemaining: Double = 0.0
        private var legDistanceRemaining: Double = 0.0
        private var legDurationRemaining: Double = 0.0
        private var stepDistanceRemaining: Double = 0.0

        fun route(route: Route) =
            apply { this.directionsRoute = route }

        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }

        fun distanceRemaining(distanceRemaining: Double) =
            apply { this.distanceRemaining = distanceRemaining }

        fun currentStepPoints(currentStepPoints: List<Point>?) =
            apply { this.currentStepPoints = currentStepPoints }

        fun bannerInstruction(bannerInstruction: BannerInstruction?) =
            apply { this.bannerInstruction = bannerInstruction }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
            apply { this.upcomingStepPoints = upcomingStepPoints }

        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }

        fun currentState(currentState: RouteProgressStateNavigation) =
            apply { this._currentState = currentState }

        fun routeGeometryWithBuffer(routeGeometryWithBuffer: Geometry?) =
            apply { this.routeGeometryWithBuffer = routeGeometryWithBuffer }

        fun currentStep(currentStep: LegStepNavigation?) = apply { this.currentStep = currentStep }

        fun stepIndex(stepIndex: Int) = apply { this.stepIndex = stepIndex }

        fun legDistanceRemaining(legDistanceRemaining: Double) =
            apply { this.legDistanceRemaining = legDistanceRemaining }

        fun stepDistanceRemaining(stepDistanceRemaining: Double) =
            apply { this.stepDistanceRemaining = stepDistanceRemaining }

        fun legDurationRemaining(legDurationRemaining: Double) =
            apply { this.legDurationRemaining = legDurationRemaining }

        private fun validate() {
            var missing = ""
            if (!this::_currentState.isInitialized) {
                missing += " currentState"
            }
            if (!this::_currentLegProgress.isInitialized) {
                missing += " currentLegProgress"
            }
            check(missing.isEmpty()) { "RouteProgressNavigation.Builder missing required properties: $missing" }
        }

        fun build(): RouteProgressNavigation {
            val leg: RouteLegNavigation? = directionsRoute?.let { directionRoute ->
                directionRoute.legs?.let { legs ->
                    legs[legIndex]
                }
            }
            val routeLegProgressBuilder = RouteLegProgressNavigation.Builder()
            ifNonNull(leg) {
                routeLegProgressBuilder.routeLeg(it)
            }
            ifNonNull(currentStep) {
                routeLegProgressBuilder.currentStep(it)
            }
            val legProgress = routeLegProgressBuilder
                .stepIndex(stepIndex)
                .distanceRemaining(legDistanceRemaining)
                .durationRemaining(legDurationRemaining)
                .stepDistanceRemaining(stepDistanceRemaining)
                .currentStepPoints(currentStepPoints)
                .upcomingStepPoints(upcomingStepPoints)
                .build()
            this._currentLegProgress = legProgress
            validate()

            return RouteProgressNavigation(
                directionsRoute,
                currentStep,
                currentStepPoints,
                upcomingStepPoints,
                routeGeometryWithBuffer,
                bannerInstruction,
                _currentState,
                _currentLegProgress,
                legIndex,
                stepIndex,
                inTunnel,
                distanceRemaining,
                legDistanceRemaining,
                legDurationRemaining,
                stepDistanceRemaining,
                this
            )
        }
    }

    override fun toString(): String {
        return route.toString() +
            currentStep.toString() +
            currentStepPoints.toString() +
            upcomingStepPoints.toString() +
            routeGeometryWithBuffer.toString() +
            bannerInstruction.toString() +
            currentState.toString() +
            currentLegProgress.toString() +
            legIndex.toString() +
            stepIndex.toString() +
            inTunnel.toString() +
            distanceRemaining.toString() +
            legDistanceRemaining.toString() +
            legDurationRemaining.toString() +
            stepDistanceRemaining.toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other is RouteProgressNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
