package com.mapbox.navigation.testing.utils.location

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.replay.route.ReplayRouteOptions
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.collections.forEach
import kotlin.math.abs

suspend fun BaseCoreNoCleanUpTest.stayOnPosition(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    times: Int,
    frequencyHz: Int = 1,
) {
    repeat(times) {
        mockLocationUpdatesRule.pushLocationUpdate {
            this.latitude = latitude
            this.longitude = longitude
            this.bearing = bearing
        }
        delay(1000L / frequencyHz)
    }
}

suspend fun <T> BaseCoreNoCleanUpTest.stayOnPosition(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    frequencyHz: Int = 1,
    block: suspend () -> T,
): T {
    return coroutineScope {
        val updateLocations = launch(start = CoroutineStart.UNDISPATCHED) {
            while (true) {
                mockLocationUpdatesRule.pushLocationUpdate {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.bearing = bearing
                }
                delay(1000L / frequencyHz)
            }
        }
        return@coroutineScope try {
            block()
        } finally {
            updateLocations.cancel()
        }
    }
}

suspend fun <T> BaseCoreNoCleanUpTest.stayOnPosition(
    location: Location,
    frequencyHz: Int = 1,
    block: suspend () -> T,
): T {
    return stayOnPosition(
        latitude = location.latitude,
        longitude = location.longitude,
        bearing = location.bearing,
        frequencyHz = frequencyHz,
        block = block,
    )
}

suspend fun <T> BaseCoreNoCleanUpTest.stayOnPosition(
    point: Point,
    bearing: Float,
    frequencyHz: Int = 1,
    block: suspend () -> T,
): T {
    return stayOnPosition(
        latitude = point.latitude(),
        longitude = point.longitude(),
        bearing = bearing,
        frequencyHz = frequencyHz,
        block = block,
    )
}

suspend fun <T> BaseCoreNoCleanUpTest.stayOnPositionAndWaitForUpdate(
    mapboxNavigation: MapboxNavigation,
    latitude: Double,
    longitude: Double,
    bearing: Float,
    frequencyHz: Int = 1,
    block: suspend () -> T,
) {
    return stayOnPosition(latitude, longitude, bearing, frequencyHz) {
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(
                it.enhancedLocation.latitude - latitude,
            ) < 0.0001 && abs(
                it.enhancedLocation.longitude - longitude,
            ) < 0.0001
        }.first()
        block()
    }
}

/**
 * @param minEventsCount count of RouteProgress events to receive until return
 */
suspend fun MapboxNavigation.moveAlongTheRouteUntilTracking(
    route: NavigationRoute,
    mockLocationReplayerRule: MockLocationReplayerRule,
    minEventsCount: Int = 1,
    endReplay: Boolean = true,
) {
    mockLocationReplayerRule.playRoute(route.directionsRoute)
    flowRouteProgress()
        .filter { it.currentState == RouteProgressState.TRACKING }
        .take(minEventsCount).toList()

    if (endReplay) mockLocationReplayerRule.stopAndClearEvents()
}

suspend fun BaseCoreNoCleanUpTest.followGeometry(
    polyline6Geometry: String,
    replayOptions: ReplayRouteOptions = ReplayRouteOptions.Builder().build(),
    block: suspend () -> Unit,
) {
    val replayer = MapboxReplayer()
    val mapper = ReplayRouteMapper(replayOptions)
    val eventsToPlay = mapper.mapGeometry(polyline6Geometry)
    replayer.pushEvents(eventsToPlay)
    replayer.play()
    replayer.registerObserver { events ->
        events.forEach {
            if (it is ReplayEventUpdateLocation) {
                mockLocationUpdatesRule.pushLocationUpdate(
                    mockLocationUpdatesRule.generateLocationUpdate {
                        setUpLocation(it)
                    }
                )
            }
        }
    }
    try {
        block()
    } finally {
        replayer.stop()
    }
}
