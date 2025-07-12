package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.utils.normalizeBearing
import com.mapbox.turf.TurfMeasurement
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal class ReplayRouteInterpolator {

    private val routeSmoother = ReplayRouteSmoother()

    /**
     * Given a list of coordinates on a route, detect sections of the route that have significant
     * turns. Return a smaller list of locations that have calculated speeds.
     */
    fun createSpeedProfile(
        options: ReplayRouteOptions,
        distinctPoints: List<Point>,
    ): List<ReplayRouteLocation> {
        val smoothLocations = routeSmoother.smoothRoute(distinctPoints, SMOOTH_THRESHOLD_METERS)
        smoothLocations.first().speedMps = 0.0
        smoothLocations.last().speedMps = 0.0

        createSpeedForTurns(options, smoothLocations)
        reduceSpeedForDistances(options, smoothLocations)

        return smoothLocations
    }

    /**
     * Interpolate a speed across a distance. The returned segment will include approximate
     * speed and distance calculations that can be used to create location coordinates.
     *
     * This will speed up to a cruising speed and slow down to the end speed, which allows for the
     * start and end speeds to be zero.
     */
    fun interpolateSpeed(
        options: ReplayRouteOptions,
        startSpeed: Double,
        endSpeed: Double,
        distance: Double,
    ): ReplayRouteSegment {
        val segment = calculateSegmentSpeedAndDistances(options, startSpeed, endSpeed, distance)
        val speedSteps = mutableListOf<ReplayRouteStep>()
            .addAcceleratingSteps(
                frequency = options.frequency,
                startSpeed = startSpeed,
                endSpeed = segment.maxSpeedMps,
                distance = segment.speedUpDistance,
            )
            .addCruisingSteps(
                frequency = options.frequency,
                speed = segment.maxSpeedMps,
                distance = segment.cruiseDistance,
            )
            .addAcceleratingSteps(
                frequency = options.frequency,
                startSpeed = segment.maxSpeedMps,
                endSpeed = endSpeed,
                distance = segment.slowDownDistance,
            )
            .clampLastStep(endSpeed, distance)

        return segment.copy(steps = speedSteps)
    }

    /**
     * A [ReplayRouteSegment] is created from a [startSpeed], [endSpeed], and a [distance]. The
     * [startSpeed] and [endSpeed] can be equal, so this determines a proper speed that can be
     * used for traveling the segment. The calling function, is responsible for calculating and
     * adding the [ReplayRouteSegment.steps] before it is returned.
     *
     * Note that it is possible to give this function an infeasible task. For example, a driver
     * cannot accelerate from 0-100mps in 10 meters with a max acceleration of 3mps^2. The
     * caller is responsible for ensuring the values are feasible.
     *
     * @param options contains thresholds like max speed and min|max acceleration
     * @param startSpeed starting speed for the segment in meters per second
     * @param endSpeed ending speed for the segment in meters per second
     * @param distance distance in meters to travel from a [startSpeed] to an [endSpeed]
     */
    private fun calculateSegmentSpeedAndDistances(
        options: ReplayRouteOptions,
        startSpeed: Double,
        endSpeed: Double,
        distance: Double,
    ): ReplayRouteSegment {
        // Solving for v with a v^2 problem requires the quadratic formula.
        // This is the result, sorry it's a mess.
        val alpha = 1.0 / options.maxAcceleration - 1.0 / options.minAcceleration
        val beta = -startSpeed.pow(2.0) / options.maxAcceleration
        val gamma = endSpeed.pow(2.0) / options.minAcceleration
        val delta = (beta + gamma - 2.0 * distance)
        var maxSpeed = sqrt(-4.0 * alpha * delta) / (2.0 * alpha)
        maxSpeed = max(max(startSpeed, endSpeed), min(options.maxSpeedMps, maxSpeed))

        // Plug back into the equations we solved for to get the maxSpeed. This will give distances.
        val t1 = (maxSpeed - startSpeed) / options.maxAcceleration
        val speedUpDistance = newtonDistance(t1, 0.0, startSpeed, options.maxAcceleration)
        val slowDownDistance = if (maxSpeed > endSpeed) {
            val t3 = (endSpeed - maxSpeed) / options.minAcceleration
            newtonDistance(t3, 0.0, maxSpeed, options.minAcceleration)
        } else {
            0.0
        }
        val cruiseDistance = (distance - (speedUpDistance + slowDownDistance)).removeZeroError()

        return ReplayRouteSegment(
            startSpeedMps = startSpeed,
            maxSpeedMps = maxSpeed,
            endSpeedMps = endSpeed,
            totalDistance = distance,
            speedUpDistance = speedUpDistance,
            cruiseDistance = cruiseDistance,
            slowDownDistance = slowDownDistance,
            steps = emptyList(),
        )
    }

    private fun MutableList<ReplayRouteStep>.addCruisingSteps(
        frequency: Double,
        speed: Double,
        distance: Double,
    ) = apply {
        if (distance == 0.0) return this
        val startDistance = lastOrNull()?.positionMeters ?: 0.0
        val startTime = lastOrNull()?.timeSeconds ?: 0.0
        if (startDistance > 0.0) { removeAt(lastIndex) }
        val t1 = distance / speed
        val steps = ceil(t1) * frequency
        val increment = t1 / steps
        for (i in 0..steps.toInt()) {
            val t = i * increment
            val replayRouteStep = ReplayRouteStep(
                timeSeconds = startTime + t,
                acceleration = 0.0,
                speedMps = speed,
                positionMeters = startDistance + speed * t,
            )
            add(replayRouteStep)
        }
    }

    private fun MutableList<ReplayRouteStep>.addAcceleratingSteps(
        frequency: Double,
        startSpeed: Double,
        endSpeed: Double,
        distance: Double,
    ) = apply {
        if (distance == 0.0) return this
        val startDistance = lastOrNull()?.positionMeters ?: 0.0
        val startTime = lastOrNull()?.timeSeconds ?: 0.0
        if (startDistance > 0.0) { removeAt(lastIndex) }
        val acceleration = (endSpeed.pow(2.0) - startSpeed.pow(2.0)) / (2 * distance)
        val t1 = (endSpeed - startSpeed) / acceleration
        val steps = ceil(t1) * frequency
        val increment = t1 / steps
        for (i in 0..steps.toInt()) {
            val t = i * increment
            val replayRouteStep = ReplayRouteStep(
                timeSeconds = startTime + t,
                acceleration = acceleration,
                speedMps = startSpeed + acceleration * t,
                positionMeters = newtonDistance(t, startDistance, startSpeed, acceleration),
            )
            add(replayRouteStep)
        }
    }

    /**
     * Clamp the end to remove any residual floating point error.
     */
    private fun MutableList<ReplayRouteStep>.clampLastStep(
        endSpeed: Double,
        distance: Double,
    ) = apply {
        val lastStep = last().copy(
            speedMps = endSpeed,
            positionMeters = distance,
        )
        set(lastIndex, lastStep)
    }

    /**
     * A little helper method to convert known values into a distance.
     *
     * @param t time in seconds
     * @param r0 start distance in meters
     * @param v0 start velocity in meters per second
     * @param a acceleration in meters per second squared
     */
    private fun newtonDistance(t: Double, r0: Double, v0: Double, a: Double): Double {
        return (r0 + v0 * t + 0.5 * a * t.pow(2.0)).removeZeroError()
    }

    /**
     * Given a list of replay locations, update each of their bearings to
     * point to the next location in the route.
     */
    fun createBearingProfile(replayRouteLocations: List<ReplayRouteLocation>) {
        if (replayRouteLocations.size < 2) return
        val lookAhead = 2
        var bearing = normalizeBearing(
            TurfMeasurement.bearing(
                replayRouteLocations[0].point,
                replayRouteLocations[1].point,
            ),
        )
        replayRouteLocations.forEachIndexed { index, location ->
            val nextIndex = min(index + lookAhead, replayRouteLocations.lastIndex)
            if (index < nextIndex) {
                val fromPoint = location.point
                val toPoint = replayRouteLocations[nextIndex].point
                bearing = normalizeBearing(TurfMeasurement.bearing(fromPoint, toPoint))
            }
            location.bearing = bearing
        }
    }

    private fun createSpeedForTurns(
        options: ReplayRouteOptions,
        smoothLocations: List<ReplayRouteLocation>,
    ) {
        for (i in 1 until smoothLocations.lastIndex) {
            val segmentStart = smoothLocations[i - 1]
            val segmentEnd = smoothLocations[i]
            val deltaBearing = abs(segmentStart.bearing - segmentEnd.bearing)
            val speedMps = when (deltaBearing) {
                in maxSpeedBearingRange -> {
                    options.maxSpeedMps
                }
                in uTurnBearingRange -> {
                    options.uTurnSpeedMps
                }
                else -> {
                    val velocityFraction = (1.0 - min(1.0, deltaBearing / 90.0)).pow(2.0)
                    val offsetToMaxVelocity = options.maxSpeedMps - options.turnSpeedMps
                    (options.turnSpeedMps + (velocityFraction * offsetToMaxVelocity))
                }
            }
            smoothLocations[i].speedMps = speedMps
        }
    }

    private fun reduceSpeedForDistances(
        options: ReplayRouteOptions,
        smoothLocations: List<ReplayRouteLocation>,
    ) {
        // Check the speed estimates with their speeds. Reduce the speed to ensure the estimates
        // are within the acceleration limits. For example, a car can only go so fast, and slamming
        // the breaks can only slow you down by so much.
        var i = 1
        while (i <= smoothLocations.lastIndex) {
            val from = smoothLocations[i - 1].speedMps
            val to = smoothLocations[i].speedMps
            val runway = smoothLocations[i - 1].distance
            val isSlowing = from - to > 0
            val acceleration = if (isSlowing) options.minAcceleration else options.maxAcceleration
            val feasibleDistance = (to.pow(2.0) - from.pow(2.0)) / (2.0 * acceleration)
            val isFeasible = (runway - feasibleDistance).removeZeroError() >= 0.0
            if (!isFeasible) {
                if (isSlowing) {
                    val d2a = runway * 2.0 * acceleration
                    val feasibleFromSpeed = sqrt(-d2a + to.pow(2.0))
                    smoothLocations[i - 1].speedMps = feasibleFromSpeed
                    // We changed a location of the previous, so we need to go back until all
                    // previous steps are feasible.
                    i = max(i - 1, 1)
                } else {
                    val d2a = runway * 2.0 * acceleration
                    val feasibleToSpeed = sqrt(d2a + from.pow(2.0))
                    smoothLocations[i].speedMps = feasibleToSpeed
                }
            } else {
                i++
            }
        }
    }

    private companion object {
        /*
         * Threshold for removing curves that will not impact the speed of the driver. The
         * measurement is the centripetal distance the driver must travel before the next
         * significant route edge.
         */
        private const val SMOOTH_THRESHOLD_METERS = 1.5

        /*
         * The road curvature used to determine
         * when the driver will drive a [ReplayRouteOptions.maxSpeedMps].
         */
        private val maxSpeedBearingRange: ClosedRange<Double> = 0.0..20.0

        /*
         * The road curvature used to determine
         * when the driver will drive the [ReplayRouteOptions.uTurnSpeedMps].
         */
        private val uTurnBearingRange: ClosedRange<Double> = 150.0..200.0

        /*
         * Constant epsilon for distances and floating point comparisons.
         */
        private const val ZERO_METERS_EPSILON = 0.001

        /*
         * When comparing distances there can be rounding error.
         * This removes the rounding error to create better value == 0.0 comparisons.
         */
        private fun Double.removeZeroError(): Double =
            if (abs(this) < ZERO_METERS_EPSILON) 0.0 else this
    }
}
