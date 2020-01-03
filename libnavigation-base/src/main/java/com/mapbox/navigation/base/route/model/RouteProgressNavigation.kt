package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.ifNonNull

data class RouteProgressNavigation constructor(
    var directionsRoute: Route? = null,
    var legIndex: Int? = null,
    var distanceRemaining: Double? = null,
    var currentLegProgress: RouteLegProgressNavigation? = null,
    var currentStepPoints: List<Point>? = null,
    var upcomingStepPoints: List<Point>? = null,
    var inTunnel: Boolean? = null,
    var currentState: RouteProgressStateNavigation? = null,
    var routeGeometryWithBuffer: Geometry? = null,
    var currentStep: LegStepNavigation? = null,
    var stepIndex: Int? = null,
    var legDistanceRemaining: Double? = null,
    var stepDistanceRemaining: Double? = null,
    var legDurationRemaining: Double? = null,
    var builder: Builder
) {

    /**
     * Get the route the navigation session is currently using. When a reroute occurs and a new
     * directions route gets obtained, with the next location update this directions route should
     * reflect the new route.
     *
     * @return a [DirectionsRoute] currently being used for the navigation session
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
     * Provides the current [RouteLegNavigation] the user is on.
     *
     * @return a [RouteLegNavigation] the user is currently on
     */
    fun currentLeg(): RouteLegsNavigation? =
            ifNonNull(directionsRoute()?.legs, legIndex) { routeLegs, legIndex ->
                routeLegs[legIndex]
            }

    /**
     * Total distance traveled in meters along route.
     *
     * @return a double value representing the total distance the user has traveled along the route,
     * using unit meters
     */
    fun distanceTraveled(): Double? =
            ifNonNull(directionsRoute?.distance, distanceRemaining) { distance, distanceRemaining ->
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
     */
    fun durationRemaining(): Long =
            ifNonNull(fractionTraveled(), directionsRoute()?.duration) { fractionTraveled, duration ->
                ((1 - fractionTraveled) * duration).toLong()
            } ?: 0L

    /**
     * Get the fraction traveled along the current route, this is a float value between 0 and 1 and
     * isn't guaranteed to reach 1 before the user reaches the end of the route.
     *
     * @return a double value between 0 and 1 representing the fraction the user has traveled along the
     * route
     */
    fun fractionTraveled(): Double? =
            ifNonNull(distanceTraveled(), directionsRoute()?.distance) { distanceTraveled, distance ->
                when (distance > 0) {
                    true -> {
                        (distanceTraveled / distance)
                    }
                    false -> {
                        1.0
                    }
                }
            } ?: 1.0

    /**
     * Provides the distance remaining in meters till the user reaches the end of the route.
     *
     * @return `long` value representing the distance remaining till end of route, in unit meters
     */
    fun distanceRemaining(): Double? = distanceRemaining

    /**
     * Number of waypoints remaining on the current route.
     *
     * @return integer value representing the number of way points remaining along the route
     */
    fun remainingWaypoints(): Int? =
            ifNonNull(directionsRoute?.legs, legIndex) { legs, legIndex -> legs.size - legIndex }

    /**
     * Gives a [RouteLegProgress] object with information about the particular leg the user is
     * currently on.
     *
     * @return a [RouteLegProgress] object
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
        private var directionsRoute: Route? = null
        private var legIndex: Int? = null
        private var distanceRemaining: Double? = null
        private var currentLegProgress: RouteLegProgressNavigation? = null
        private var currentStepPoints: List<Point>? = null
        private var upcomingStepPoints: List<Point>? = null
        private var inTunnel: Boolean? = null
        private var currentState: RouteProgressStateNavigation? = null
        private var routeGeometryWithBuffer: Geometry? = null
        private var currentStep: LegStepNavigation? = null
        private var stepIndex: Int? = null
        private var legDistanceRemaining: Double? = null
        private var stepDistanceRemaining: Double? = null
        private var legDurationRemaining: Double? = null

        fun directionsRoute(directionsRoute: Route) =
                apply { this.directionsRoute = directionsRoute }

        fun legIndex(legIndex: Int) = apply { this.legIndex = legIndex }

        fun distanceRemaining(distanceRemaining: Double) =
                apply { this.distanceRemaining = distanceRemaining }

        fun currentLegProgress(currentLegProgress: RouteLegProgressNavigation) =
                apply { this.currentLegProgress = currentLegProgress }

        fun currentStepPoints(currentStepPoints: List<Point>?) =
                apply { this.currentStepPoints = currentStepPoints }

        fun upcomingStepPoints(upcomingStepPoints: List<Point>?) =
                apply { this.upcomingStepPoints = upcomingStepPoints }

        fun inTunnel(inTunnel: Boolean) = apply { this.inTunnel = inTunnel }

        fun currentState(currentState: RouteProgressStateNavigation?) =
                apply { this.currentState = currentState }

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

        fun build(): RouteProgressNavigation {
            // Default values for variables that are null if not initialized
            val _legDistanceRemaining = legDistanceRemaining ?: 0.0
            val _stepIndex = stepIndex ?: 0
            val _legDurationRemaining = legDurationRemaining ?: 0.0
            val _stepDistanceRemaining = stepDistanceRemaining ?: 0.0
            val _legIndex = legIndex ?: 0

            val leg: RouteLegsNavigation? = directionsRoute?.let { directionRoute ->
                directionRoute.legs?.let { legs ->
                    legs[_legIndex]
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
                    .stepIndex(_stepIndex)
                    .distanceRemaining(_legDistanceRemaining)
                    .durationRemaining(_legDurationRemaining)
                    .stepDistanceRemaining(_stepDistanceRemaining)
                    .currentStepPoints(currentStepPoints)
                    .upcomingStepPoints(upcomingStepPoints)
                    .build()
            currentLegProgress(legProgress)
            validate()

            return RouteProgressNavigation(
                    directionsRoute,
                    legIndex,
                    distanceRemaining,
                    currentLegProgress,
                    currentStepPoints,
                    upcomingStepPoints,
                    inTunnel,
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
