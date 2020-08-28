package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.min

internal class ReplayRouteDriver {

    private val routeSmoother = ReplayRouteSmoother()
    private val routeInterpolator = ReplayRouteInterpolator()
    private val replayRouteTraffic = ReplayRouteTraffic()

    private var timeMillis = 0L

    /**
     * Given a geometry polyline, simulate the locations needed to drive the geometry.
     *
     * @param options allow you to control the driver and car behavior
     * @param geometry polyline string at [DirectionsCriteria.GEOMETRY_POLYLINE6]
     * @return [ReplayRouteLocation] [List]
     */
    fun driveGeometry(options: ReplayRouteOptions, geometry: String): List<ReplayRouteLocation> {
        val coordinates = LineString.fromPolyline(geometry, 6).coordinates()
        return drivePointList(options, coordinates)
    }

    /**
     * Given a list of location points, simulate the locations needed to drive the points.
     *
     * @param options allow you to control the driver and car behavior
     * @param points list of points describing a route
     * @return [ReplayRouteLocation] [List]
     */
    fun drivePointList(
        options: ReplayRouteOptions,
        points: List<Point>
    ): List<ReplayRouteLocation> {
        val distinctPoints = routeSmoother.distinctPoints(points)
        if (distinctPoints.size < 2) return emptyList()

        val smoothLocations = routeInterpolator.createSpeedProfile(options, distinctPoints)
        val replayRouteLocations = interpolateLocations(options, distinctPoints, smoothLocations)
        routeInterpolator.createBearingProfile(replayRouteLocations)

        return replayRouteLocations
    }

    /**
     * Given a [RouteLeg], use the [LegAnnotation] to create the speed and distance calculations.
     * To use this, your Directions request must include [DirectionsCriteria.ANNOTATION_SPEED] and
     * [DirectionsCriteria.ANNOTATION_DISTANCE].
     *
     * @param routeLeg Directions API response of a [RouteLeg].
     * @return [ReplayRouteLocation] [List]
     */
    fun driveRouteLeg(routeLeg: RouteLeg): List<ReplayRouteLocation> {
        val legAnnotation = routeLeg.annotation()
        check(
            legAnnotation != null &&
                legAnnotation.distance()?.isNotEmpty() ?: false &&
                legAnnotation.speed()?.isNotEmpty() ?: false
        ) {
            "Directions request should include annotations DirectionsCriteria.ANNOTATION_SPEED " +
                "and DirectionsCriteria.ANNOTATION_DISTANCE"
        }
        val options = ReplayRouteOptions.Builder().build()
        val routePoints = replayRouteTraffic.mapToDistinctRoutePoints(routeLeg)
        val trafficLocations = replayRouteTraffic.trafficLocations(
            routePoints,
            legAnnotation.distance()!!,
            legAnnotation.speed()!!
        )
        val replayRouteLocations = driveTraffic(options, routePoints, trafficLocations)
        routeInterpolator.createBearingProfile(replayRouteLocations)

        return replayRouteLocations
    }

    private fun driveTraffic(
        options: ReplayRouteOptions,
        routePoints: List<Point>,
        trafficLocations: List<ReplayRouteLocation>
    ): List<ReplayRouteLocation> {
        val replayRouteLocations = mutableListOf<ReplayRouteLocation>()
        var segmentStart = replayRouteLocations.addFirstTrafficLocation(routePoints)
        for (i in 0..trafficLocations.lastIndex) {
            val segmentEnd = trafficLocations[i]
            val segmentRoute = routeSmoother.segmentRoute(
                routePoints,
                segmentStart.routeIndex!!,
                segmentEnd.routeIndex!!
            )
            val segmentMaxSpeed = min(segmentEnd.speedMps, options.maxSpeedMps)
            val segmentOptions = options.toBuilder().maxSpeedMps(segmentMaxSpeed).build()
            replayRouteLocations.addInterpolatedLocations(
                segmentOptions,
                segmentRoute,
                segmentStart,
                segmentEnd
            )
            segmentStart = segmentEnd
        }

        return replayRouteLocations
    }

    private fun MutableList<ReplayRouteLocation>.addFirstTrafficLocation(
        routePoints: List<Point>
    ): ReplayRouteLocation {
        val segmentStart = ReplayRouteLocation(0, routePoints[0])
        segmentStart.speedMps = 0.0
        addLocation(segmentStart)
        return segmentStart
    }

    private fun interpolateLocations(
        options: ReplayRouteOptions,
        distinctPoints: List<Point>,
        smoothLocations: List<ReplayRouteLocation>
    ): List<ReplayRouteLocation> {
        val replayRouteLocations = mutableListOf<ReplayRouteLocation>()
        replayRouteLocations.addLocation(smoothLocations.first())

        for (i in 0 until smoothLocations.lastIndex) {
            val segmentStart = smoothLocations[i]
            val segmentEnd = smoothLocations[i + 1]
            val segmentRoute = routeSmoother.segmentRoute(
                distinctPoints,
                segmentStart.routeIndex!!,
                segmentEnd.routeIndex!!
            )
            replayRouteLocations.addInterpolatedLocations(
                options,
                segmentRoute,
                segmentStart,
                segmentEnd
            )
        }

        return replayRouteLocations
    }

    private fun MutableList<ReplayRouteLocation>.addInterpolatedLocations(
        options: ReplayRouteOptions,
        segmentRoute: List<Point>,
        segmentStart: ReplayRouteLocation,
        segmentEnd: ReplayRouteLocation
    ) {
        val segmentDistance = TurfMeasurement.length(segmentRoute, TurfConstants.UNIT_METERS)
        val segment = routeInterpolator.interpolateSpeed(
            options,
            segmentStart.speedMps,
            segmentEnd.speedMps,
            segmentDistance
        )

        for (stepIndex in 1..segment.steps.lastIndex) {
            val step = segment.steps[stepIndex]
            val point =
                TurfMeasurement.along(segmentRoute, step.positionMeters, TurfConstants.UNIT_METERS)
            val location = ReplayRouteLocation(null, point)
            location.distance = step.positionMeters
            location.speedMps = step.speedMps
            addLocation(location)
        }
    }

    private fun MutableList<ReplayRouteLocation>.addLocation(location: ReplayRouteLocation) {
        location.timeMillis = timeMillis
        add(location)
        timeMillis += 1000L
    }
}
