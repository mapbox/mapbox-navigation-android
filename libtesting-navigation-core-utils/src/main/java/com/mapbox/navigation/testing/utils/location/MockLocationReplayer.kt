package com.mapbox.navigation.testing.utils.location

import android.location.Location
import android.os.SystemClock
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.internal.MapboxReplayerFactory
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.testing.ui.MockLocationUpdatesRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Date

/**
 * A rule that allows for injecting route replay samples into the [MockLocationUpdatesRule].
 *
 * Create this rule and then start a route with [playRoute].
 */
class MockLocationReplayerRule(mockLocationUpdatesRule: MockLocationUpdatesRule) : TestWatcher() {
    private val replayEventsObserver = object : ReplayEventsObserver {
        override fun replayEvents(events: List<ReplayEventBase>) {
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
    }
    private val mapper = ReplayRouteMapper()
    private var mapboxReplayer: MapboxReplayer? = null

    override fun starting(description: Description?) {
        mapboxReplayer = MapboxReplayerFactory.create().also {
            it.registerObserver(replayEventsObserver)
        }
    }

    override fun finished(description: Description?) {
        mapboxReplayer?.finish()
        mapboxReplayer = null
    }

    fun playRoute(
        directionsRoute: DirectionsRoute,
        eventsToDrop: Int = 0
    ) {
        val replayEvents = mapper.mapDirectionsRouteGeometry(directionsRoute)
            .drop(eventsToDrop)
        mapboxReplayer?.clearEvents()
        mapboxReplayer?.pushEvents(replayEvents)
        mapboxReplayer?.seekTo(replayEvents.first())
        mapboxReplayer?.play()
    }

    fun playGeometry(
        polyLine6Geometry: String,
    ) {
        val replayEvents = mapper.mapGeometry(polyLine6Geometry)
        mapboxReplayer?.clearEvents()
        mapboxReplayer?.pushEvents(replayEvents)
        mapboxReplayer?.seekTo(replayEvents.first())
        mapboxReplayer?.play()
    }

    suspend fun loopUpdateUntil(location: Location, stopper: suspend () -> Unit) {
        loopUpdate(location, 120)
        stopper()
        mapboxReplayer?.run {
            stop()
            clearEvents()
        }
    }

    fun loopUpdate(location: Location, times: Int) {
        val events: List<ReplayEventUpdateLocation> =
            mutableListOf<ReplayEventUpdateLocation>().apply {
                repeat(times) {
                    this.add(location.toReplayEventUpdateLocation(it.toDouble()))
                }
            }
        mapboxReplayer?.run {
            stop()
            clearEvents()
            pushEvents(events)
            seekTo(events.first())
            play()
        }
    }

    fun stopAndClearEvents() {
        mapboxReplayer?.run {
            stop()
            clearEvents()
        }
    }
}

fun Location.setUpLocation(event: ReplayEventUpdateLocation) {
    val eventLocation = event.location
    this.longitude = eventLocation.lon
    this.latitude = eventLocation.lat
    this.time = Date().time
    this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    eventLocation.accuracyHorizontal?.toFloat()?.let { this.accuracy = it }
    eventLocation.bearing?.toFloat()?.let { this.bearing = it }
    eventLocation.altitude?.let { this.altitude = it }
    eventLocation.speed?.toFloat()?.let { this.speed = it }
}

fun Location.toReplayEventUpdateLocation(
    timestamp: Double
): ReplayEventUpdateLocation {
    return ReplayEventUpdateLocation(
        timestamp,
        ReplayEventLocation(
            this.longitude,
            this.latitude,
            this.provider,
            this.time.toDouble(),
            this.altitude,
            this.accuracy.toDouble(),
            this.bearing.toDouble(),
            this.speed.toDouble()
        )
    )
}
