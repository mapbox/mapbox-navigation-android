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
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.TripSessionResetCallback
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
    private val routesObserver = slot<RoutesObserver>()
    private val routeProgressObserver = slot<RouteProgressObserver>()
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { mapboxReplayer } returns replayer
        every { navigationOptions } returns options
        every { registerRoutesObserver(capture(routesObserver)) } just runs
        every { registerRouteProgressObserver(capture(routeProgressObserver)) } just runs
        every { resetTripSession(any()) } answers {
            firstArg<TripSessionResetCallback>().onTripSessionReset()
        }
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
        mockkStatic(PermissionsManager::class)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns bestLocationEngine
    }

    @After
    fun teardown() {
        unmockkAll()
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
            mapboxNavigation.resetTripSession(any())
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
    fun `onAttached - should push first device location if enabled`() {
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
            mapboxNavigation.startReplayTripSession()
            replayer.play()
            replayer.pushEvents(any())
        }
        val capturedLocation = (replayEventsSlot.captured[0] as ReplayEventUpdateLocation)
        assertEquals(1.0, capturedLocation.location.lat, 0.0)
        assertEquals(-2.0, capturedLocation.location.lon, 0.0)
    }

    @Test
    fun `onAttached - should not push first device location if disabled`() {
        every { bestLocationEngine.getLastLocation(any()) } just runs
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        val replayEventsSlot = slot<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(replayEventsSlot)) } returns replayer

        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(false).build())
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) {
            bestLocationEngine.getLastLocation(any())
        }
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
    fun `onDetached - should stop and clear the replayer`() {
        sut.onDetached(mapboxNavigation)

        verifyOrder {
            replayer.unregisterObserver(any())
            replayer.stop()
            replayer.clearEvents()
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
        sut.setOptions(
            ReplayRouteSessionOptions.Builder()
                .decodeMinDistance(1.0)
                .build()
        )

        sut.onAttached(mapboxNavigation)
        routeProgressObserver.captured.onRouteProgressChanged(mockRouteProgress())

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
        val routeProgress = mockRouteProgress()

        sut.setOptions(
            ReplayRouteSessionOptions.Builder()
                .decodeMinDistance(0.001)
                .build()
        )
        sut.onAttached(mapboxNavigation)
        routeProgressObserver.captured.onRouteProgressChanged(routeProgress)

        // Verify every point in the geometry was simulated
        val pushedPoints = pushedEvents.flatten().toList().map {
            val location = (it as ReplayEventUpdateLocation).location
            Point.fromLngLat(location.lon, location.lat)
        }
        val geometry = routeProgress.navigationRoute.directionsRoute.geometry()!!
        val geometryPoints = PolylineUtils.decode(geometry, 6)
        assertTrue(
            "${pushedPoints.size} > ${geometryPoints.size}",
            pushedPoints.size > geometryPoints.size
        )
        assertTrue(
            geometryPoints.all { lhs ->
                pushedPoints.firstOrNull { rhs -> lhs.equals(rhs) } != null
            }
        )
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
        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns emptyList() }
        )
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
            mapboxNavigation.startReplayTripSession()
            replayer.play()
            replayer.pushEvents(any())
        }
        val capturedLocation = (replayEventsSlot.captured[0] as ReplayEventUpdateLocation)
        assertEquals(1.0, capturedLocation.location.lat, 0.0)
        assertEquals(-2.0, capturedLocation.location.lon, 0.0)
    }

    @Test
    fun `onAttached registered listeners should be unregistered onDetached`() {
        val progressObserver = slot<RouteProgressObserver>()
        val routesObserver = slot<RoutesObserver>()
        val replayEventsObserver = slot<ReplayEventsObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(replayEventsObserver)) } just runs

        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.registerRouteProgressObserver(any())
            mapboxNavigation.registerRoutesObserver(any())
            replayer.registerObserver(any())
            mapboxNavigation.unregisterRoutesObserver(routesObserver.captured)
            mapboxNavigation.unregisterRouteProgressObserver(progressObserver.captured)
            replayer.unregisterObserver(replayEventsObserver.captured)
        }
    }

    @Test
    fun `onAttached - should skip to the current routeProgress distanceTraveled`() {
        val progressObserver = slot<RouteProgressObserver>()
        val routesObserver = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        val activeRoutes = mockActiveRoutesUpdatedResult()
        val primaryRoute = activeRoutes.navigationRoutes.first()

        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(activeRoutes)
        progressObserver.captured.onRouteProgressChanged(
            mockk {
                every { navigationRoute } returns primaryRoute
                every { currentRouteGeometryIndex } returns 15
            }
        )

        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        verifySkipToIndex(pushedEvents.captured, primaryRoute, 15)
    }

    @Test
    fun `onAttached - should skip to short routeProgress currentRouteGeometryIndex`() {
        val progressObserver = slot<RouteProgressObserver>()
        val routesObserver = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        val activeRoutes = mockActiveRoutesUpdatedResult()
        val primaryRoute = activeRoutes.navigationRoutes.first()

        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(activeRoutes)
        progressObserver.captured.onRouteProgressChanged(
            mockk {
                every { navigationRoute } returns primaryRoute
                every { currentRouteGeometryIndex } returns 12
            }
        )

        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        verifySkipToIndex(pushedEvents.captured, primaryRoute, 12)
    }

    @Test
    fun `onRouteProgress - will change to new route when the route changes`() {
        val progressObserver = slot<RouteProgressObserver>()
        val routesObserver = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        val firstRoutesUpdatedResult = mockActiveRoutesUpdatedResult()
        val firstRoute = firstRoutesUpdatedResult.navigationRoutes.first()
        every { firstRoute.id } returns "test-first-route-id"
        val firstRouteProgress = mockk<RouteProgress> {
            every { navigationRoute } returns firstRoute
            every { currentRouteGeometryIndex } returns 12
        }
        val secondRoutesUpdatedResult = mockActiveRoutesUpdatedResult()
        val secondRoute = secondRoutesUpdatedResult.navigationRoutes.first()
        every { secondRoute.id } returns "test-second-route-id"
        val secondRouteProgress = mockk<RouteProgress> {
            every { navigationRoute } returns secondRoute
            every { currentRouteGeometryIndex } returns 13
        }

        sut.onAttached(mapboxNavigation)
        clearAllMocks(answers = false)
        routesObserver.captured.onRoutesChanged(firstRoutesUpdatedResult)
        progressObserver.captured.onRouteProgressChanged(firstRouteProgress)
        progressObserver.captured.onRouteProgressChanged(firstRouteProgress)
        progressObserver.captured.onRouteProgressChanged(firstRouteProgress)
        routesObserver.captured.onRoutesChanged(secondRoutesUpdatedResult)
        progressObserver.captured.onRouteProgressChanged(secondRouteProgress)

        verify(exactly = 2) {
            replayer.clearEvents()
            replayer.pushEvents(any())
        }
        verifyOrder {
            replayer.clearEvents()
            replayer.play()
            replayer.pushEvents(any())
            replayer.clearEvents()
            replayer.play()
            replayer.pushEvents(any())
        }
    }

    private fun verifySkipToIndex(
        pushedEvents: List<ReplayEventBase>,
        primaryRoute: NavigationRoute,
        currentRouteGeometryIndex: Int
    ) {
        val geometry = primaryRoute.directionsRoute.geometry()!!
        val fullRoute = PolylineUtils.decode(geometry, 6)
        val expected = fullRoute[currentRouteGeometryIndex]
        val firstReplayLocation = (pushedEvents.first() as ReplayEventUpdateLocation).location
        val firstReplayPoint = Point.fromLngLat(firstReplayLocation.lon, firstReplayLocation.lat)

        assertEquals(expected, firstReplayPoint)
    }

    private fun mockActiveRoutesUpdatedResult(): RoutesUpdatedResult = mockk {
        every { navigationRoutes } returns listOf(mockNavigationRoute())
    }

    private fun mockRouteProgress(): RouteProgress = mockk {
        every { navigationRoute } returns mockNavigationRoute()
        every { currentRouteGeometryIndex } returns 0
    }

    private fun mockNavigationRoute(): NavigationRoute = mockk {
        val geometry = "_kmbgAppafhFwXaOuC}ApAoEbNqe@jAaEhEcOtAwEdAoD`DaLnCiJ|M_e@`Je[rAyEnEgO" +
            "tGiUxByHlDjBp@^zKdG`Ah@`HtDx@d@rGlDl@\\pAp@dAl@p@^nItEpQvJfAh@fDjB`D`Br@`@nKpFbDhB" +
            "~KlGtDvBvAwE|EqPzFeSvHaXtA{ElAiE|@_D"
        every { id } returns "test-navigation-route-id"
        every { routeOptions }
        every { directionsRoute } returns mockk {
            every { routeOptions() } returns mockk {
                every { geometries() } returns "polyline6"
                every { geometry() } returns geometry
            }
        }
    }
}
