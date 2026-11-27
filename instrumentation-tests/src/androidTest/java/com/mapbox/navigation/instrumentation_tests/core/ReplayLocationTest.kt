package com.mapbox.navigation.instrumentation_tests.core

import android.os.SystemClock
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.rawLocationUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.ApproximateCoordinates
import com.mapbox.navigation.testing.utils.location.toReplayEventUpdateLocation
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMapboxNavigationAPI::class)
class ReplayLocationTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private val tolerance = 0.000001

    // Covers the delay between a location being produced and being observed by the test.
    private val maxDeviceClockOffsetMillis = 5_000.0

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private val realLocation = ApproximateCoordinates(0.9, 0.9, tolerance)

    override fun setupMockLocation(): android.location.Location =
        mockLocationUpdatesRule.generateLocationUpdate {
            latitude = realLocation.latitude
            longitude = realLocation.longitude
        }

    @Test
    fun replay_session_locations_do_not_contain_locations_from_previous_session() = sdkTest {
        withMapboxNavigation { mapboxNavigation ->
            val firstReplayApproximateLocation = ApproximateCoordinates(1.0, 1.0, tolerance)
            val secondReplayApproximateLocation = ApproximateCoordinates(1.2, 1.2, tolerance)
            val rawLocations = mutableListOf<ApproximateCoordinates>()
            val locationObserver = object : LocationObserver {
                override fun onNewRawLocation(rawLocation: Location) {
                    rawLocations.add(
                        ApproximateCoordinates(
                            rawLocation.latitude,
                            rawLocation.longitude,
                            tolerance,
                        ),
                    )
                }

                override fun onNewLocationMatcherResult(
                    locationMatcherResult: LocationMatcherResult,
                ) {
                }
            }
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.startReplayTripSession()
            updateReplayLocation(
                mapboxNavigation,
                mockLocationUpdatesRule.generateLocationUpdate {
                    latitude = firstReplayApproximateLocation.latitude
                    longitude = firstReplayApproximateLocation.longitude
                },
            )
            mapboxNavigation.rawLocationUpdates()
                .filter {
                    ApproximateCoordinates(
                        it.latitude,
                        it.longitude,
                        tolerance,
                    ) == firstReplayApproximateLocation
                }
                .first()
            mapboxNavigation.stopTripSession()
            assertEquals(List(rawLocations.size) { firstReplayApproximateLocation }, rawLocations)
            rawLocations.clear()

            mapboxNavigation.startTripSession()
            loopRealUpdate(realLocation, 120)
            mapboxNavigation.rawLocationUpdates()
                .filter {
                    ApproximateCoordinates(it.latitude, it.longitude, tolerance) == realLocation
                }
                .first()
            mapboxNavigation.stopTripSession()
            assertEquals(List(rawLocations.size) { realLocation }, rawLocations)
            rawLocations.clear()

            mapboxNavigation.startReplayTripSession()
            updateReplayLocation(
                mapboxNavigation,
                mockLocationUpdatesRule.generateLocationUpdate {
                    latitude = secondReplayApproximateLocation.latitude
                    longitude = secondReplayApproximateLocation.longitude
                },
            )
            mapboxNavigation.rawLocationUpdates()
                .filter {
                    ApproximateCoordinates(it.latitude, it.longitude, tolerance) ==
                        secondReplayApproximateLocation
                }
                .first()
            assertEquals(
                List(rawLocations.size) { secondReplayApproximateLocation },
                rawLocations,
            )
        }
    }

    @Test
    fun replay_started_during_regular_session_keeps_locations_on_device_clock() = sdkTest {
        withMapboxNavigation { mapboxNavigation ->
            val replayApproximateLocation = ApproximateCoordinates(1.0, 1.0, tolerance)

            mapboxNavigation.startTripSession()
            loopRealUpdate(realLocation, 120)
            val regularLocationClockOffsetMillis = mapboxNavigation.rawLocationUpdates()
                .filter {
                    ApproximateCoordinates(it.latitude, it.longitude, tolerance) == realLocation
                }
                .map { it.offsetFromDeviceClockMillis() }
                .first()

            // The regular session is not stopped: replay takes over a session that is running.
            mapboxNavigation.startReplayTripSession()
            updateReplayLocation(
                mapboxNavigation,
                mockLocationUpdatesRule.generateLocationUpdate {
                    latitude = replayApproximateLocation.latitude
                    longitude = replayApproximateLocation.longitude
                },
            )
            val replayLocationClockOffsetMillis = mapboxNavigation.rawLocationUpdates()
                .filter {
                    ApproximateCoordinates(it.latitude, it.longitude, tolerance) ==
                        replayApproximateLocation
                }
                .map { it.offsetFromDeviceClockMillis() }
                .first()

            assertEquals(
                "a regular location should be stamped close to the device clock",
                0.0,
                regularLocationClockOffsetMillis.toDouble(),
                maxDeviceClockOffsetMillis,
            )
            assertEquals(
                "a replayed location should be stamped close to the device clock",
                0.0,
                replayLocationClockOffsetMillis.toDouble(),
                maxDeviceClockOffsetMillis,
            )
        }
    }

    /**
     * How far the location's monotonic timestamp is from the device clock at the moment
     * the location is observed. Positive when the location is stamped in the future.
     */
    private fun Location.offsetFromDeviceClockMillis(): Long {
        val locationTimeNanos = requireNotNull(monotonicTimestamp) {
            "location is expected to have a monotonic timestamp"
        }
        return TimeUnit.NANOSECONDS.toMillis(locationTimeNanos - SystemClock.elapsedRealtimeNanos())
    }

    private fun updateReplayLocation(
        mapboxNavigation: MapboxNavigation,
        location: android.location.Location,
    ) {
        val events = listOf(location.toReplayEventUpdateLocation(0.0))
        mapboxNavigation.mapboxReplayer.run {
            stop()
            clearEvents()
            pushEvents(events)
            seekTo(events.first())
            play()
        }
    }

    private fun loopRealUpdate(approximateCoordinates: ApproximateCoordinates, times: Int) {
        repeat(times) {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = approximateCoordinates.latitude
                longitude = approximateCoordinates.longitude
            }
        }
    }
}
