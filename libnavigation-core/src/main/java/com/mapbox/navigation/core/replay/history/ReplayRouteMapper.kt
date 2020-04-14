package com.mapbox.navigation.core.replay.history

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
     * Map a directions route into replay events.
     *
     * @param directionsRoute that is converted in
     * @return replay events for the [ReplayHistoryPlayer]
     */
    fun mapToUpdateLocations(directionsRoute: DirectionsRoute): List<ReplayEventBase> {
        val geometry = directionsRoute.geometry() ?: return emptyList()
        val lineString = LineString.fromPolyline(geometry, 6) ?: return emptyList()
        val startTime = 0.0
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
     * @param location Android location to be replayed
     * @return a singleton list to be replayed, otherwise an empty list if the location cannot be replayed.
     */
    fun mapToUpdateLocation(location: Location?): List<ReplayEventBase> {
        location ?: return emptyList()
        val replayAtTimeMillis = 0
        val replayAtTimeSecond = replayAtTimeMillis * 1e-4
        val updateLocationEvent = ReplayEventUpdateLocation(
            eventTimestamp = replayAtTimeSecond,
            location = ReplayEventLocation(
                lat = location.longitude,
                lon = location.latitude,
                provider = LOCATION_PROVIDER_REPLAY_ROUTE,
                time = replayAtTimeSecond,
                altitude = location.altitude,
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
