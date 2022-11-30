package com.mapbox.navigation.core.replay.route

import android.content.Context
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayRouteSessionTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val context: Context = mockk(relaxed = true)
    private val replayer: MapboxReplayer = mockk(relaxed = true)
    private val options: NavigationOptions = mockk {
        every { applicationContext } returns context
    }
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { mapboxReplayer } returns replayer
        every { navigationOptions } returns options
    }
    private val bestLocationEngine: LocationEngine = mockk {
        every { getLastLocation(any()) } just runs
    }

    private val sut = ReplayRouteSession().setOptions(
        ReplayRouteSessionOptions.Builder()
            .locationResetEnabled(false)
            .build()
    )

    @Before
    fun setup() {
        // todo this will have to be changed to `mockkStatic(PermissionsManager::class)`
        // when upgrading to Common SDK v23.2.0
        mockkObject(PermissionsManager)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns bestLocationEngine
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached - should stop trip session and start replay session`() {
        sut.onAttached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.stopTripSession()
            mapboxNavigation.startReplayTripSession()
            replayer.play()
        }
    }

    @Test
    fun `onAttached - should reset trip session and replayer when navigation routes are cleared`() {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } returns Unit
        sut.onAttached(mapboxNavigation)

        routesObserver.captured.apply {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
            }
            onRoutesChanged(result)
        }

        verifyOrder {
            replayer.clearEvents()
            mapboxNavigation.resetTripSession()
            replayer.play()
        }
    }

    @Test
    fun `onAttached - should register ReplayEventsObserver`() {
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            replayer.registerObserver(capture(eventsObserver))
        } returns Unit
        sut.onAttached(mapboxNavigation)

        assertTrue(eventsObserver.isCaptured)
    }

    @Test
    fun `onDetached - should unregister ReplayEventsObserver`() {
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            replayer.unregisterObserver(capture(eventsObserver))
        } returns Unit
        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        assertTrue(eventsObserver.isCaptured)
    }

    @Test
    fun `onDetached - should stop trip session and replayer`() {
        sut.onDetached(mapboxNavigation)

        verifyOrder {
            replayer.stop()
            replayer.clearEvents()
            mapboxNavigation.stopTripSession()
        }
    }

    @Test
    fun `ReplayRouteOptions - can be changed before onAttached`() {
        val initialOptions = sut.getOptions()
        val firstOptions = mockk<ReplayRouteSessionOptions>()
        sut.setOptions(firstOptions)

        assertNotEquals(firstOptions, initialOptions)
        assertEquals(firstOptions, sut.getOptions())
    }

    @Test
    fun `ReplayRouteOptions - can be changed after onAttached`() {
        val initialOptions = sut.getOptions()
        val firstOptions = mockk<ReplayRouteSessionOptions> {
            every { replayRouteOptions } returns mockk()
        }
        sut.onAttached(mapboxNavigation)
        sut.setOptions(firstOptions)

        assertNotEquals(firstOptions, initialOptions)
        assertEquals(firstOptions, sut.getOptions())
    }

    @Test
    fun `onAttached - should push the initial batch of events`() {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        sut.setOptions(
            ReplayRouteSessionOptions.Builder()
                .decodeMinDistance(1.0)
                .build()
        )

        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(mockActiveNavigationRoutes())

        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        assertTrue("5 < ${pushedEvents.captured.size}", 5 < pushedEvents.captured.size)
    }

    @Test
    fun `onAttached - should request a mapping between every location`() {
        val routesObserver = slot<RoutesObserver>()
        val replayEventsObserver = slot<ReplayEventsObserver>()
        val pushedEvents = mutableListOf<List<ReplayEventBase>>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        every { replayer.registerObserver(capture(replayEventsObserver)) } just runs
        every { replayer.pushEvents(capture(pushedEvents)) } answers {
            replayEventsObserver.captured.replayEvents(firstArg())
            replayer
        }
        val routesUpdatedResult = mockActiveNavigationRoutes()

        sut.setOptions(
            ReplayRouteSessionOptions.Builder()
                .decodeMinDistance(0.001)
                .build()
        )
        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(routesUpdatedResult)

        // Verify every point in the geometry was simulated
        val pushedPoints = pushedEvents.flatten().toList().map {
            val location = (it as ReplayEventUpdateLocation).location
            Point.fromLngLat(location.lon, location.lat)
        }
        val geometry = routesUpdatedResult.navigationRoutes.first().directionsRoute.geometry()!!
        val geometryPoints = PolylineUtils.decode(geometry, 6)
        assertTrue(pushedPoints.size > geometryPoints.size)
        assertTrue(
            geometryPoints.all { lhs ->
                pushedPoints.firstOrNull { rhs -> lhs.equals(rhs) } != null
            }
        )
    }

    @Test
    fun `onAttached - should request gps location when resetLocationEnabled is true`() {
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(true).build())
        sut.onAttached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.stopTripSession()
            mapboxNavigation.startReplayTripSession()
            bestLocationEngine.getLastLocation(any())
            replayer.play()
        }
    }

    @Test
    fun `onAttached - should push gps location when route is not set`() {
        val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
        every { bestLocationEngine.getLastLocation(capture(locationCallbackSlot)) } just runs
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        val replayEventsSlot = slot<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(replayEventsSlot)) } returns replayer

        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(true).build())
        sut.onAttached(mapboxNavigation)
        locationCallbackSlot.captured.onSuccess(
            mockk {
                every { lastLocation } returns mockk(relaxed = true) {
                    every { latitude } returns 1.0
                    every { longitude } returns -2.0
                    every { provider } returns "ReplayRouteSessionTest"
                }
            }
        )

        verifyOrder {
            mapboxNavigation.stopTripSession()
            mapboxNavigation.startReplayTripSession()
            replayer.play()
            replayer.pushEvents(any())
        }
        val capturedLocation = (replayEventsSlot.captured[0] as ReplayEventUpdateLocation)
        assertEquals(1.0, capturedLocation.location.lat, 0.0)
        assertEquals(-2.0, capturedLocation.location.lon, 0.0)
    }

    private fun mockActiveNavigationRoutes(): RoutesUpdatedResult = mockk {
        val geometry = "_kmbgAppafhFwXaOuC}ApAoEbNqe@jAaEhEcOtAwEdAoD`DaLnCiJ|M_e@`Je[rAyEnEgO" +
            "tGiUxByHlDjBp@^zKdG`Ah@`HtDx@d@rGlDl@\\pAp@dAl@p@^nItEpQvJfAh@fDjB`D`Br@`@nKpFbDhB" +
            "~KlGtDvBvAwE|EqPzFeSvHaXtA{ElAiE|@_D"
        every { navigationRoutes } returns listOf(
            mockk {
                every { id } returns "test-navigation-route-id"
                every { routeOptions }
                every { directionsRoute } returns mockk {
                    every { routeOptions() } returns mockk {
                        every { geometries() } returns "polyline6"
                        every { geometry() } returns geometry
                    }
                }
            }
        )
    }
}
