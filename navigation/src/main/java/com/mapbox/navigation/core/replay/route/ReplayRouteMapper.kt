package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.utils.internal.logW

/**
 * This class converts a [DirectionsRoute] into events that can be
 * replayed using the [MapboxReplayer] to navigate a route.
 */
class ReplayRouteMapper @JvmOverloads constructor(
    /**
     * Options that allow you to control the driver and car behavior
     */
    var options: ReplayRouteOptions = ReplayRouteOptions.Builder().build(),
) {

    private val replayRouteDriver = ReplayRouteDriver()

    /**
     * Take a [DirectionsRoute] and map it to events that can be replayed by the [MapboxReplayer].
     * Uses the Directions API [DirectionsRoute.geometry] to calculate the speed
     * and position estimates for the replay locations.
     *
     * @param directionsRoute the [DirectionsRoute] containing information about a route
     * @return [ReplayEventBase] [List]
     */
    fun mapDirectionsRouteGeometry(directionsRoute: DirectionsRoute): List<ReplayEventBase> {
        val geometries = directionsRoute.routeOptions()?.geometries()
        val usesPolyline6 = geometries?.contains(DirectionsCriteria.GEOMETRY_POLYLINE6) ?: false
        if (!usesPolyline6) {
            logW(
                "Make sure that the route's geometry is encoded with polyline6'",
                LOG_CATEGORY,
            )
        }
        directionsRoute.geometry() ?: return emptyList()
        return mapPointList(directionsRoute.completeGeometryToPoints())
    }

    /**
     * Take a single [RouteLeg] from the [DirectionsRoute] and map it to a drive using the
     * [LegStep.geometry] composed together.
     *
     * @param routeLeg the [RouteLeg] that is converted to replay events
     * @return [ReplayEventBase] [List]
     */
    fun mapRouteLegGeometry(routeLeg: RouteLeg): List<ReplayEventBase> {
        val replayEvents = mutableListOf<ReplayEventBase>()
        routeLeg.steps()?.flatMap { legStep ->
            val geometry = legStep.geometry() ?: return emptyList()
            PolylineUtils.decode(geometry, 6)
        }?.also { points ->
            replayRouteDriver.drivePointList(options, points)
                .map { mapToUpdateLocation(it) }
                .forEach { replayEvents.add(it) }
        }
        return replayEvents
    }

    /**
     * Simulate a driver navigating a polyline string.
     *
     * @param geometry is a [DirectionsCriteria.GEOMETRY_POLYLINE6]
     * @return [ReplayEventBase] [List]
     */
    fun mapGeometry(geometry: String): List<ReplayEventBase> {
        return mapPointList(PolylineUtils.decode(geometry, 6))
    }

    /**
     * Take a list of [Point] and map it to events that can be replayed by the [MapboxReplayer].
     *
     * @param points containing location coordinates to be replayed.
     * @return [ReplayEventBase] [List]
     */
    fun mapPointList(points: List<Point>): List<ReplayEventBase> {
        return replayRouteDriver.drivePointList(options, points)
            .map { mapToUpdateLocation(it) }
    }

    /**
     * Take a [DirectionsRoute] and map it to events that can be replayed by the [MapboxReplayer].
     * Uses the Directions API [LegAnnotation] to create the speed and position
     * estimates for the replay locations.
     *
     * @param directionsRoute the [DirectionsRoute] containing information about a route
     * @return [ReplayEventBase] [List]
     */
    fun mapDirectionsRouteLegAnnotation(directionsRoute: DirectionsRoute): List<ReplayEventBase> {
        return directionsRoute.legs()?.flatMap { routeLeg ->
            mapRouteLegAnnotation(routeLeg)
        } ?: emptyList()
    }

    /**
     * Given a [RouteLeg], use the [LegAnnotation] to create the speed and locations.
     * To use this, your Directions request must include [DirectionsCriteria.ANNOTATION_SPEED] and
     * [DirectionsCriteria.ANNOTATION_DISTANCE]
     *
     * @param routeLeg the [RouteLeg] to be mapped into replay events
     * @return [ReplayEventBase] [List]
     */
    fun mapRouteLegAnnotation(routeLeg: RouteLeg): List<ReplayEventBase> {
        return replayRouteDriver.driveRouteLeg(routeLeg)
            .map { mapToUpdateLocation(it) }
    }

    companion object {
        private const val LOG_CATEGORY = "ReplayRouteMapper"
        private const val REPLAY_ROUTE_ACCURACY_HORIZONTAL = 3.0

        /**
         * Map a point into a replay event.
         *
         * @param eventTimestamp the eventTimestamp for the replay event
         * @param point [Point] location to be replayed
         * @return a [ReplayEventBase] event that can be replayed
         */
        @JvmStatic
        fun mapToUpdateLocation(eventTimestamp: Double, point: Point): ReplayEventUpdateLocation {
            return ReplayEventUpdateLocation(
                eventTimestamp = eventTimestamp,
                location = ReplayEventLocation(
                    lon = point.longitude(),
                    lat = point.latitude(),
                    provider = "ReplayRoute",
                    time = eventTimestamp,
                    altitude = null,
                    accuracyHorizontal = null,
                    bearing = null,
                    speed = null,
                ),
            )
        }

        /**
         * Map an Android location into a replay event.
         *
         * @param eventTimestamp the eventTimestamp for the replay event
         * @param location Android location to be replayed
         * @return a [ReplayEventBase] event that can be replayed
         */
        @JvmStatic
        fun mapToUpdateLocation(eventTimestamp: Double, location: Location): ReplayEventBase {
            return ReplayEventUpdateLocation(
                eventTimestamp = eventTimestamp,
                location = ReplayEventLocation(
                    lon = location.longitude,
                    lat = location.latitude,
                    provider = location.source,
                    time = eventTimestamp,
                    altitude = location.altitude,
                    accuracyHorizontal = location.horizontalAccuracy,
                    bearing = location.bearing,
                    speed = location.speed,
                ),
            )
        }

        /**
         * Map an Android location into a replay event.
         *
         * @param location simulated location used for replay
         * @return a [ReplayEventBase] event that can be replayed
         */
        internal fun mapToUpdateLocation(location: ReplayRouteLocation): ReplayEventBase {
            return ReplayEventUpdateLocation(
                eventTimestamp = location.timeSeconds,
                location = ReplayEventLocation(
                    lon = location.point.longitude(),
                    lat = location.point.latitude(),
                    provider = "ReplayRoute",
                    time = location.timeSeconds,
                    altitude = null,
                    accuracyHorizontal = REPLAY_ROUTE_ACCURACY_HORIZONTAL,
                    bearing = location.bearing,
                    speed = location.speedMps,
                ),
            )
        }
    }
}
