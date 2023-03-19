package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.ApproximateCoordinates
import com.mapbox.navigation.instrumentation_tests.utils.location.toReplayEventUpdateLocation
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.rawLocationUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayLocationTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    private lateinit var mapboxNavigation: MapboxNavigation
    private val tolerance = 0.000001

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private val realLocation = ApproximateCoordinates(0.9, 0.9, tolerance)

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = realLocation.latitude
        longitude = realLocation.longitude
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
        }
    }

    @Test
    fun replay_session_locations_do_not_contain_locations_from_previous_session() = sdkTest {
        val firstReplayApproximateLocation = ApproximateCoordinates(1.0, 1.0, tolerance)
        val secondReplayApproximateLocation = ApproximateCoordinates(1.2, 1.2, tolerance)
        val rawLocations = mutableListOf<ApproximateCoordinates>()
        val locationObserver = object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {
                rawLocations.add(
                    ApproximateCoordinates(
                        rawLocation.latitude,
                        rawLocation.longitude,
                        tolerance
                    )
                )
            }

            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            }
        }
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.startReplayTripSession()
        updateReplayLocation(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = firstReplayApproximateLocation.latitude
                longitude = firstReplayApproximateLocation.longitude
            }
        )
        mapboxNavigation.rawLocationUpdates()
            .filter {
                ApproximateCoordinates(
                    it.latitude,
                    it.longitude,
                    tolerance
                ) == firstReplayApproximateLocation
            }
            .first()
        mapboxNavigation.stopTripSession()
        assertEquals(List(rawLocations.size) { firstReplayApproximateLocation }, rawLocations)
        rawLocations.clear()

        mapboxNavigation.startTripSession()
        loopRealUpdate(realLocation, 120)
        mapboxNavigation.rawLocationUpdates()
            .filter { ApproximateCoordinates(it.latitude, it.longitude, tolerance) == realLocation }
            .first()
        mapboxNavigation.stopTripSession()
        assertEquals(List(rawLocations.size) { realLocation }, rawLocations)
        rawLocations.clear()

        mapboxNavigation.startReplayTripSession()
        updateReplayLocation(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = secondReplayApproximateLocation.latitude
                longitude = secondReplayApproximateLocation.longitude
            }
        )
        mapboxNavigation.rawLocationUpdates()
            .filter {
                ApproximateCoordinates(it.latitude, it.longitude, tolerance) ==
                    secondReplayApproximateLocation
            }
            .first()
        assertEquals(List(rawLocations.size) { secondReplayApproximateLocation }, rawLocations)
    }

    private fun updateReplayLocation(location: Location) {
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
