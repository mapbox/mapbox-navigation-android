package com.mapbox.navigation.core.replay.history

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import java.util.*
import kotlin.math.max


/**
 * This class converts a directions rout into events that can be
 * replayed using the [ReplayHistoryPlayer] to navigate a route.
 */
class ReplayRouteMapper : RouteProgressObserver {

    private var replayEventsListener: ReplayEventsListener? = null

    private var directionsRoute: DirectionsRoute? = null
    private var currentLeg: Int = 0
    private var currentStep: Int = 0

    private fun speedMpsFromKph(speedKph: Double): Double {
        val oneKmInMeters = 1000.0
        val oneHourInSeconds = 3600
        return speedKph * oneKmInMeters / oneHourInSeconds
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val stepPointsSize = routeProgress.currentLegProgress()?.currentStepProgress()?.stepPoints()?.size
        val stepDistanceRemaining = routeProgress.currentLegProgress()?.currentStepProgress()?.distanceRemaining()
        val legDistanceRemaining = routeProgress.currentLegProgress()?.distanceRemaining()
        Log.i("ReplayRoute", "ReplayRoute $legDistanceRemaining $stepDistanceRemaining $stepPointsSize")
    }

    fun observeReplayEvents(directionsRoute: DirectionsRoute, replayEventsListener: ReplayEventsListener) {
        this.directionsRoute = directionsRoute
        currentLeg = 0
        currentStep = 0
        this.replayEventsListener = replayEventsListener
        val routeLeg = directionsRoute.legs()?.firstOrNull()
            ?: return
        val replayEvents = mapToUpdateLocations(0.0, routeLeg)
        Log.i("ReplayRoute", "ReplayRoute first leg has events ${replayEvents.size}")
        replayEventsListener(replayEvents)
    }

    val maxSpeedMps = speedMpsFromKph(45.0)

    fun mapToUpdateLocations(startTime: Double, routeLeg: RouteLeg): List<ReplayEventBase> {
        val updateLocationEvents = mutableListOf<ReplayEventBase>()
        val firstStep = routeLeg.steps()?.firstOrNull() ?: return emptyList()
        val geometry = firstStep.geometry() ?: return emptyList()
        val lineString = LineString.fromPolyline(geometry, 6) ?: return emptyList()

        var currentTime = startTime

        val coordinates = lineString.coordinates()
        for (i in 0 until coordinates.size - 1) {
            val fromPoint = coordinates[i]
            val toPoint = coordinates[i+1]
            val distance = TurfMeasurement.distance(fromPoint, toPoint, TurfConstants.UNIT_METERS)
            val bearing = TurfMeasurement.bearing(fromPoint, toPoint)

            var speedMps = 0.0
            var nextDistance = 0.0
            var accelerationMps2 = 3.0
            var currentPoint = fromPoint
            val addLocationEvent: () -> Unit = {
                updateLocationEvents.add(ReplayEventUpdateLocation(
                    eventTimestamp = currentTime,
                    location = ReplayEventLocation(
                        lon = currentPoint.longitude(),
                        lat = currentPoint.latitude(),
                        provider = LOCATION_PROVIDER_REPLAY_ROUTE,
                        time = currentTime,
                        altitude = null,
                        accuracyHorizontal = 1.0,
                        bearing = bearing,
                        speed = speedMps
                    )
                ))
                currentTime += 1.0
                speedMps = max(maxSpeedMps, speedMps + accelerationMps2)
                nextDistance += speedMps
            }
            addLocationEvent.invoke()

            while (nextDistance < distance) {
                currentPoint = TurfMeasurement.destination(fromPoint, nextDistance, bearing, TurfConstants.UNIT_METERS)
                addLocationEvent.invoke()
            }

            currentPoint = toPoint
            addLocationEvent.invoke()
        }

//        val interpolatedDistance = 0.0
//        while (interpolatedDistance <= distance) {
//            var travelled = 0.0
//            for (i in firstStep.indices) {
//                travelled += if (distance >= travelled && i == coords.size - 1) {
//                    break
//                } else if (travelled >= distance) {
//                    val overshot = distance - travelled
//                    return if (overshot == 0.0) {
//                        coords.get(i)
//                    } else {
//                        val direction = TurfMeasurement.bearing(coords.get(i), coords.get(i - 1)) - 180
//                        TurfMeasurement.destination(coords.get(i), overshot, direction, units)
//                    }
//                } else {
//                    TurfMeasurement.distance(coords.get(i), coords.get(i + 1), units)
//                }
//            }
//
//            return coords.get(coords.size - 1)
//        }
        return updateLocationEvents
    }

//    fun mapToUpdateLocations(startTime: Double, ): List<ReplayEventBase> {
//        val updateLocationEvents = mutableListOf<ReplayEventBase>()
//        sliceRoute(startMps = , lineString).fold(startTime) { replayTimeSecond, point ->
//            val distance = TurfMeasurement.distance(lastPoint, point, TurfConstants.UNIT_METERS)
//            val bearing = if (distance < 0.1) null else TurfMeasurement.bearing(lastPoint, point)
//            val deltaSeconds = 1.0
//            val speed = if (distance < 0.1) 0.0 else distance / deltaSeconds
//            val replayAtTimeSecond = replayTimeSecond + deltaSeconds
//            val updateLocationEvent = ReplayEventUpdateLocation(
//                eventTimestamp = replayAtTimeSecond,
//                location = ReplayEventLocation(
//                    lon = point.longitude(),
//                    lat = point.latitude(),
//                    provider = LOCATION_PROVIDER_REPLAY_ROUTE,
//                    time = replayAtTimeSecond,
//                    altitude = null,
//                    accuracyHorizontal = null,
//                    bearing = bearing,
//                    speed = speed
//                )
//            )
//            updateLocationEvents.add(updateLocationEvent)
//            lastPoint = point
//            replayAtTimeSecond
//        }
//
//        return updateLocationEvents.toList()
//    }


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
