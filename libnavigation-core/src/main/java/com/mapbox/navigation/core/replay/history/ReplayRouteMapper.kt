package com.mapbox.navigation.core.replay.history

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.Collections

/**
 * This class converts a directions rout into events that can be
 * replayed using the [ReplayHistoryPlayer] to navigate a route.
 */
class ReplayRouteMapper(
    /**
     * Determines the spacing of the replay locations by kilometers per hour
     */
    private var speedKph: Int = DEFAULT_REPLAY_SPEED_KPH
) {
    private val distancePerSecondMps by lazy {
        val oneKmInMeters = 1000.0
        val oneHourInSeconds = 3600
        speedKph.toDouble() * oneKmInMeters / oneHourInSeconds
    }

    /**
     * Map a [DirectionsRoute] route into replay events.
     *
     * @param startTime the minimum eventTimestamp in the replay events
     * @param directionsRoute used to be converted into replay events
     * @return replay events for the [ReplayHistoryPlayer]
     */
    fun mapToUpdateLocations(startTime: Double, directionsRoute: DirectionsRoute): List<ReplayEventBase> {
        val routeEvents = mutableListOf<ReplayEventBase>()
        var currentTime = startTime
        directionsRoute.legs()?.forEach { routeLeg ->
            val routeLegEvents = mapToUpdateLocations(currentTime, routeLeg)
            currentTime = routeLegEvents.lastOrNull()?.eventTimestamp ?: currentTime
            routeEvents.addAll(routeLegEvents)
        }
        return routeEvents
    }

    /**
     * Map a [RouteLeg] route into replay events.
     *
     * @param startTime the minimum eventTimestamp in the replay events
     * @param routeLeg used to be converted into replay events
     * @return replay events for the [ReplayHistoryPlayer]
     */
    fun mapToUpdateLocations(startTime: Double, routeLeg: RouteLeg): List<ReplayEventBase> {
        val routeLegEvents = mutableListOf<ReplayEventBase>()
        var currentTime = startTime
        routeLeg.steps()?.forEach {
            val stepGeometry = it.geometry() ?: return routeLegEvents
            val stepEvents = mapToUpdateLocations(currentTime, stepGeometry)
            currentTime = routeLegEvents.lastOrNull()?.eventTimestamp ?: currentTime
            routeLegEvents.addAll(stepEvents)
        }
        return routeLegEvents
    }

    /**
     * Map a geometry into replay events.
     *
     * @param startTime the minimum eventTimestamp in the replay events
     * @param geometry polyline string with precision 6
     * @return replay events for the [ReplayHistoryPlayer]
     */
    fun mapToUpdateLocations(startTime: Double, geometry: String): List<ReplayEventBase> {
        val lineString = LineString.fromPolyline(geometry, 6) ?: return emptyList()
        val updateLocationEvents = mutableListOf<ReplayEventBase>()
        var lastPoint = lineString.coordinates().first()
        sliceRoute(lineString).fold(startTime) { replayTimeSecond, point ->
            val distance = TurfMeasurement.distance(lastPoint, point, TurfConstants.UNIT_METERS)
            val bearing = if (distance < 0.1) null else TurfMeasurement.bearing(lastPoint, point)
            val deltaSeconds = 1.0
            val speed = if (distance < 0.1) 0.0 else distance / deltaSeconds
            val replayAtTimeSecond = replayTimeSecond + deltaSeconds
            val updateLocationEvent = ReplayEventUpdateLocation(
                eventTimestamp = replayAtTimeSecond,
                location = ReplayEventLocation(
                    lon = point.longitude(),
                    lat = point.latitude(),
                    provider = LOCATION_PROVIDER_REPLAY_ROUTE,
                    time = replayAtTimeSecond,
                    altitude = null,
                    accuracyHorizontal = null,
                    bearing = bearing,
                    speed = speed
                )
            )
            updateLocationEvents.add(updateLocationEvent)
            lastPoint = point
            replayAtTimeSecond
        }

        return updateLocationEvents.toList()
    }

    // Evenly distributes the route into points that assume a constant speed throughout the route.
    // This method causes known issues because actual navigation does not have constant speed.
    // Alternatives for varying speed depending on maneuvers is not yet supported. 
    private fun sliceRoute(lineString: LineString): List<Point> {
        val distanceMeters = TurfMeasurement.length(lineString, TurfConstants.UNIT_METERS)
        if (distanceMeters <= 0) {
            return emptyList()
        }

        val points = ArrayList<Point>()
        var i = 0.0
        while (i < distanceMeters) {
            val point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS)
            points.add(point)
            i += distancePerSecondMps
        }
        return points
    }

    /**
     * Map a Android location into a replay event.
     *
     * @param eventTimestamp the eventTimestamp for the replay event
     * @param location Android location to be replayed
     * @return a singleton list to be replayed, otherwise an empty list if the location cannot be replayed.
     */
    fun mapToUpdateLocation(eventTimestamp: Double, location: Location?): List<ReplayEventBase> {
        location ?: return emptyList()
        val updateLocationEvent = ReplayEventUpdateLocation(
            eventTimestamp = eventTimestamp,
            location = ReplayEventLocation(
                lon = location.longitude,
                lat = location.latitude,
                provider = LOCATION_PROVIDER_REPLAY_ROUTE,
                time = eventTimestamp,
                altitude = if (location.hasAltitude()) location.altitude else null,
                accuracyHorizontal = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
                bearing = if (location.hasBearing()) location.bearing.toDouble() else null,
                speed = if (location.hasSpeed()) location.speed.toDouble() else null
            )
        )
        return Collections.singletonList(updateLocationEvent)
    }

    companion object {
        private const val DEFAULT_REPLAY_SPEED_KPH = 45
        private const val LOCATION_PROVIDER_REPLAY_ROUTE = "ReplayRouteMapper"
    }
}
