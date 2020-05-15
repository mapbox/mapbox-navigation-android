package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

internal class ReplayRouteInterpolator {

    private val routeSmoother = ReplayRouteSmoother()

    /**
     * Given a list of coordinates on a route, detect sections of the route that have significant
     * turns. Return a smaller list of locations that have calculated speeds.
     */
    fun createSpeedProfile(options: ReplayRouteOptions, distinctPoints: List<Point>): List<ReplayRouteLocation> {
        val smoothLocations = routeSmoother.smoothRoute(distinctPoints, SMOOTH_THRESHOLD_METERS)
        smoothLocations.first().speedMps = 0.0
        smoothLocations.last().speedMps = 0.0

        createSpeedForTurns(options, smoothLocations)
        reduceSpeedForDistances(options, smoothLocations)

        return smoothLocations
    }

    /**
     * Interpolate a speed across a distance. The returned segment will include approximate
     * once per second speed and distance calculations that can be used to create location coordinates.
     */
    fun interpolateSpeed(options: ReplayRouteOptions, startSpeed: Double, endSpeed: Double, distance: Double): ReplayRouteSegment {
        val speedSteps = mutableListOf<ReplayRouteStep>()
        speedSteps.add(ReplayRouteStep(
            acceleration = 0.0,
            speedMps = startSpeed,
            positionMeters = 0.0
        ))

        while (speedSteps.last().positionMeters < distance) {
            estimateSpeedStep(options, speedSteps, endSpeed, distance)
        }

        return ReplayRouteSegment(
            startSpeedMps = startSpeed,
            endSpeedMps = endSpeed,
            distanceMeters = distance,
            steps = speedSteps)
    }

    /**
     * Given a list of replay locations, update each of their bearings to
     * point to the next location in the route.
     */
    fun createBearingProfile(replayRouteLocations: List<ReplayRouteLocation>) {
        var bearing = replayRouteLocations.first().bearing
        val lookAhead = 2
        replayRouteLocations.forEachIndexed { index, location ->
            if (index + lookAhead < replayRouteLocations.lastIndex) {
                val fromPoint = location.point
                val toPoint = replayRouteLocations[index + lookAhead].point
                bearing = TurfMeasurement.bearing(fromPoint, toPoint)
            }
            location.bearing = bearing
        }
    }

    private fun estimateSpeedStep(options: ReplayRouteOptions, speedSteps: MutableList<ReplayRouteStep>, endSpeed: Double, distance: Double) {
        val previous = speedSteps.last()
        val acceleration = maxSpeedForStep(options, previous)

        val needsToSlowDown = isRemainingStepSlowingDown(options, previous, endSpeed, acceleration, distance)
        if (needsToSlowDown) {
            interpolateSlowdown(options, endSpeed, distance, speedSteps)
        } else {
            val replayRouteStep = newtonsNextReplayRouteStep(acceleration, previous)
            speedSteps.add(replayRouteStep)
        }
    }

    private fun createSpeedForTurns(options: ReplayRouteOptions, smoothLocations: List<ReplayRouteLocation>) {
        for (i in 1 until smoothLocations.lastIndex) {
            val deltaBearing = abs(smoothLocations[i - 1].bearing - smoothLocations[i].bearing)
            val speedMps = if (deltaBearing > 150) {
                options.uTurnSpeedMps
            } else {
                val velocityFraction = 1.0 - min(1.0, deltaBearing / 90.0)
                val offsetToMaxVelocity = options.maxSpeedMps - options.turnSpeedMps
                (options.turnSpeedMps + (velocityFraction * offsetToMaxVelocity))
            }
            smoothLocations[i].speedMps = speedMps
        }
    }

    private fun reduceSpeedForDistances(options: ReplayRouteOptions, smoothLocations: List<ReplayRouteLocation>) {
        for (i in smoothLocations.lastIndex downTo 1) {
            val to = smoothLocations[i].speedMps
            val from = smoothLocations[i - 1].speedMps
            val runway = smoothLocations[i - 1].distance
            val isSlowingDown = (to - from) < 0.0
            if (isSlowingDown) {
                val runwayNeeded = distanceToSlowDown(options, from, 0.0, to)
                if (runwayNeeded > runway) {
                    smoothLocations[i - 1].speedMps = maxSpeedForDistance(options, to, runway)
                }
            }
        }
    }

    private fun isRemainingStepSlowingDown(options: ReplayRouteOptions, previousStep: ReplayRouteStep, endSpeed: Double, acceleration: Double, distance: Double): Boolean {
        val slowDownDistance = distanceToSlowDown(options, previousStep.speedMps, acceleration, endSpeed)
        val nextRemainingDistance = distance - (previousStep.positionMeters + previousStep.speedMps)
        return nextRemainingDistance > 0 && nextRemainingDistance <= slowDownDistance
    }

    private fun newtonsNextReplayRouteStep(acceleration: Double, previousStep: ReplayRouteStep): ReplayRouteStep {
        val speed = previousStep.speedMps + acceleration
        val position = previousStep.positionMeters + (speed + previousStep.speedMps) / 2.0
        return ReplayRouteStep(
            acceleration = acceleration,
            speedMps = speed,
            positionMeters = position
        )
    }

    private fun maxSpeedForStep(options: ReplayRouteOptions, previousStep: ReplayRouteStep): Double {
        return when {
            previousStep.speedMps == options.maxSpeedMps -> 0.0
            previousStep.speedMps > options.maxSpeedMps -> options.maxSpeedMps - previousStep.speedMps
            else -> min(options.maxSpeedMps - previousStep.speedMps, options.maxAcceleration)
        }
    }

    /**
     * Assumes we need to slow down to [endSpeed] in [distance] meters. Add the [ReplayRouteStep]s
     * needed to perform that maneuver accurately.
     */
    private fun interpolateSlowdown(options: ReplayRouteOptions, endSpeed: Double, distance: Double, steps: MutableList<ReplayRouteStep>) {
        var previous = steps.last()
        val targetDistance = distance - previous.positionMeters
        val targetSpeed = endSpeed - previous.speedMps
        val remainingSteps = ceil(targetSpeed / options.minAcceleration).toInt() + 1
        val acceleration = targetSpeed / remainingSteps
        val positionSpeed = targetDistance / remainingSteps
        for (i in 0 until remainingSteps) {
            previous = steps.last()
            steps.add(ReplayRouteStep(
                acceleration = acceleration,
                speedMps = previous.speedMps + acceleration,
                positionMeters = previous.positionMeters + positionSpeed
            ))
        }
        steps[steps.lastIndex] = ReplayRouteStep(steps.last().acceleration, endSpeed, distance)
    }

    private fun distanceToSlowDown(options: ReplayRouteOptions, velocity: Double, acceleration: Double, endVelocity: Double): Double {
        var currentVelocity = velocity + acceleration
        var distanceToStop = 0.0
        while (currentVelocity > endVelocity) {
            val velocityNext = currentVelocity + options.minAcceleration
            distanceToStop += (velocityNext + currentVelocity) / 2.0
            currentVelocity = velocityNext
        }
        return distanceToStop
    }

    private fun maxSpeedForDistance(options: ReplayRouteOptions, endSpeed: Double, distance: Double): Double {
        var currentVelocity = endSpeed
        var distanceToStop = -options.minAcceleration
        do {
            val velocityNext = currentVelocity - options.minAcceleration
            distanceToStop += (velocityNext + currentVelocity) / 2.0
            currentVelocity = velocityNext
        } while (distanceToStop < distance)
        return currentVelocity
    }

    companion object {
        private const val SMOOTH_THRESHOLD_METERS = 3.0
    }
}
