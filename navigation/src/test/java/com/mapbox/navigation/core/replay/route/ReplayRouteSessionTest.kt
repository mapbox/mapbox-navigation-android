package com.mapbox.navigation.core.replay.route

import android.content.Context
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.LocationServiceFactory
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
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import io.mockk.clearAllMocks
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
    private val locationProvider: DeviceLocationProvider = mockk {
        every { getLastLocation(any()) } returns Cancelable { }
    }

    private val sut = ReplayRouteSession().setOptions(
        ReplayRouteSessionOptions.Builder()
            .locationResetEnabled(false)
            .build(),
    )

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
        mockkObject(PermissionsManager)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
        mockkStatic(LocationServiceFactory::class)
        every { LocationServiceFactory.getOrCreate() } returns mockk {
            every {
                getDeviceLocationProvider(null)
            } returns ExpectedFactory.createValue(locationProvider)
        }
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
        val locationCallbackSlot = slot<GetLocationCallback>()
        every {
            locationProvider.getLastLocation(capture(locationCallbackSlot))
        } returns Cancelable { }
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        val replayEventsSlot = slot<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(replayEventsSlot)) } returns replayer

        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(true).build())
        sut.onAttached(mapboxNavigation)
        locationCallbackSlot.captured.run(
            mockk(relaxed = true) {
                every { latitude } returns 1.0
                every { longitude } returns -2.0
                every { source } returns "ReplayRouteSessionTest"
            },
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
        every { locationProvider.getLastLocation(any()) } returns Cancelable { }
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        val replayEventsSlot = slot<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(replayEventsSlot)) } returns replayer

        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(false).build())
        sut.onAttached(mapboxNavigation)

        verify(exactly = 0) {
            locationProvider.getLastLocation(any())
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
                .build(),
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
                .build(),
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
            pushedPoints.size > geometryPoints.size,
        )
        assertTrue(
            geometryPoints.all { lhs ->
                pushedPoints.firstOrNull { rhs -> lhs.equals(rhs) } != null
            },
        )
    }

    @Test
    fun `onAttached - should push gps location when route is not set`() {
        val locationCallbackSlot = slot<GetLocationCallback>()
        every {
            locationProvider.getLastLocation(capture(locationCallbackSlot))
        } returns Cancelable { }
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        val replayEventsSlot = slot<List<ReplayEventBase>>()
        every { replayer.pushEvents(capture(replayEventsSlot)) } returns replayer

        sut.setOptions(ReplayRouteSessionOptions.Builder().locationResetEnabled(true).build())
        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns emptyList() },
        )
        locationCallbackSlot.captured.run(
            mockk(relaxed = true) {
                every { latitude } returns 1.0
                every { longitude } returns -2.0
                every { source } returns "ReplayRouteSessionTest"
            },
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
        progressObserver.captured.onRouteProgressChanged(
            mockk {
                every { navigationRoute } returns primaryRoute
                every { currentRouteGeometryIndex } returns 12
            },
        )

        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        verifySkipToIndex(pushedEvents.captured, primaryRoute, 12)
    }

    @Test
    fun `onAttached - should start from index 0 when routes changes if not playing events`() {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        val activeRoutes = mockActiveRoutesUpdatedResult()
        val primaryRoute = activeRoutes.navigationRoutes.first()
        every { replayer.isPlaying() } returns false

        sut.onAttached(mapboxNavigation)
        routesObserver.captured.onRoutesChanged(activeRoutes)

        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        verifySkipToIndex(pushedEvents.captured, primaryRoute, 0)
    }

    @Test
    fun `onAttached - should not start playing route when route appears if playing events`() {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        val activeRoutes = mockActiveRoutesUpdatedResult()
        every { replayer.isPlaying() } returns true

        sut.onAttached(mapboxNavigation)

        routesObserver.captured.onRoutesChanged(activeRoutes)

        verify(exactly = 0) { replayer.pushEvents(any()) }
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
        val secondRoutesUpdatedResult = mockActiveRoutesUpdatedResult(divergingRouteGeometry)
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

    @Test
    fun `onRouteProgress - should start from a standstill for the first route`() {
        val progressObserver = slot<RouteProgressObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        val route = mockNavigationRoute()

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(route, 0))

        assertEquals(0.0, firstPushedSpeed(), 0.001)
    }

    @Test
    fun `onRouteProgress - should keep the played speed when the route changes mid-drive`() {
        val progressObserver = slot<RouteProgressObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        val secondRoute = mockNavigationRoute(divergingRouteGeometry)
        every { secondRoute.id } returns "test-second-route-id"

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        // The replayer reports a location it has played, so the driver is doing 15 m/s. The
        // eventTimestamp stays low so that this does not also request the next batch of points.
        // Keep the speed feasible for the geometry ahead, otherwise createSpeedProfile legitimately
        // reduces it and the assertion below is no longer exact.
        eventsObserver.captured.replayEvents(
            listOf(mockReplayLocation(eventTimestamp = 0.5, speedMps = 15.0, pointAfterVertex(0))),
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(secondRoute, 0))

        assertEquals(15.0, firstPushedSpeed(), 0.001)
    }

    @Test
    fun `onRouteProgress - should resume from the driver position along the new route`() {
        val progressObserver = slot<RouteProgressObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        val secondRoute = mockNavigationRoute(divergingRouteGeometry)
        every { secondRoute.id } returns "test-second-route-id"
        val driverPoint = pointAfterVertex(12)

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        eventsObserver.captured.replayEvents(
            listOf(mockReplayLocation(eventTimestamp = 0.5, speedMps = 15.0, driverPoint)),
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(secondRoute, 12))

        // Without this the drive would restart at vertex 12, behind where the driver already is.
        val pushedPoints = pushedPoints()
        assertEquals(driverPoint, pushedPoints.first())
        // From there the drive has to follow the new route, not the one that was being played.
        assertEquals(PolylineUtils.decode(divergingRouteGeometry, 6).last(), pushedPoints.last())
    }

    @Test
    fun `onRouteProgress - should not move the driver backwards when the route index is stale`() {
        val progressObserver = slot<RouteProgressObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        val secondRoute = mockNavigationRoute(divergingRouteGeometry)
        every { secondRoute.id } returns "test-second-route-id"
        // The driver has moved well past the vertex the route index still points at, which is what
        // repeated route changes produced on device: index frozen at 5 while the driver reached 20.
        val driverPoint = pointAfterVertex(20)

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        eventsObserver.captured.replayEvents(
            listOf(mockReplayLocation(eventTimestamp = 0.5, speedMps = 15.0, driverPoint)),
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(secondRoute, 5))

        // Previously this snapped back to vertex 5 with the speed reset, a jump of tens of meters.
        val pushedPoints = pushedPoints()
        assertEquals(driverPoint, pushedPoints.first())
        assertEquals(15.0, firstPushedSpeed(), 0.001)
        val newRouteVertices = PolylineUtils.decode(divergingRouteGeometry, 6)
        assertTrue(pushedPoints.all { segmentIndexOn(newRouteVertices, it) >= 20 })
    }

    @Test
    fun `onRouteProgress - should continue from the driver when an alternative shares the road`() {
        val progressObserver = slot<RouteProgressObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        // Shares the road with the first route until vertex 40, the way an alternative does.
        val alternative = mockNavigationRoute(routeDivergingAtVertex(40))
        every { alternative.id } returns "test-alternative-route-id"
        val driverPoint = pointAfterVertex(3)

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        eventsObserver.captured.replayEvents(
            listOf(mockReplayLocation(eventTimestamp = 0.5, speedMps = 15.0, driverPoint)),
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(alternative, 3))

        // The alternative is taken over right away, seamlessly: same position, same speed, and
        // the drive follows the alternative once the roads part.
        val pushedPoints = pushedPoints()
        assertEquals(driverPoint, pushedPoints.first())
        assertEquals(15.0, firstPushedSpeed(), 0.001)
        assertEquals(
            PolylineUtils.decode(routeDivergingAtVertex(40), 6).last(),
            pushedPoints.last(),
        )
    }

    @Test
    fun `onRouteProgress - should restart at the vertex when the driver is off the new route`() {
        val progressObserver = slot<RouteProgressObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        val secondRoute = mockNavigationRoute(divergingRouteGeometry)
        every { secondRoute.id } returns "test-second-route-id"

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        // Nowhere near the route, so this is a relocation rather than a continuation.
        eventsObserver.captured.replayEvents(
            listOf(
                mockReplayLocation(
                    eventTimestamp = 0.5,
                    speedMps = 15.0,
                    Point.fromLngLat(0.0, 0.0),
                ),
            ),
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(secondRoute, 12))

        assertEquals(PolylineUtils.decode(divergingRouteGeometry, 6)[12], pushedPoints().first())
        assertEquals(0.0, firstPushedSpeed(), 0.001)
    }

    @Test
    fun `onRouteProgress - should start from a standstill again after the routes are cleared`() {
        val progressObserver = slot<RouteProgressObserver>()
        val routesObserver = slot<RoutesObserver>()
        val eventsObserver = slot<ReplayEventsObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } just runs
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } just runs
        every { replayer.registerObserver(capture(eventsObserver)) } just runs
        val firstRoute = mockNavigationRoute()
        every { firstRoute.id } returns "test-first-route-id"
        val secondRoute = mockNavigationRoute(divergingRouteGeometry)
        every { secondRoute.id } returns "test-second-route-id"

        sut.onAttached(mapboxNavigation)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(firstRoute, 0))
        eventsObserver.captured.replayEvents(
            listOf(mockReplayLocation(eventTimestamp = 0.5, speedMps = 15.0, pointAfterVertex(0))),
        )
        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf() },
        )
        clearAllMocks(answers = false)
        progressObserver.captured.onRouteProgressChanged(mockRouteProgressFor(secondRoute, 0))

        assertEquals(0.0, firstPushedSpeed(), 0.001)
    }

    /** Index of the geometry segment of [vertices] that [point] is nearest to. */
    private fun segmentIndexOn(vertices: List<Point>, point: Point): Int =
        TurfMisc.nearestPointOnLine(point, vertices).getNumberProperty("index").toInt()

    private fun pushedPoints(): List<Point> {
        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        return pushedEvents.captured.map {
            val location = (it as ReplayEventUpdateLocation).location
            Point.fromLngLat(location.lon, location.lat)
        }
    }

    private fun firstPushedSpeed(): Double {
        val pushedEvents = slot<List<ReplayEventBase>>()
        verify { replayer.pushEvents(capture(pushedEvents)) }
        return (pushedEvents.captured.first() as ReplayEventUpdateLocation).location.speed!!
    }

    private fun mockRouteProgressFor(route: NavigationRoute, geometryIndex: Int): RouteProgress =
        mockk {
            every { navigationRoute } returns route
            every { currentRouteGeometryIndex } returns geometryIndex
        }

    private fun mockReplayLocation(
        eventTimestamp: Double,
        speedMps: Double,
        point: Point,
    ) = ReplayEventUpdateLocation(
        eventTimestamp = eventTimestamp,
        location = ReplayEventLocation(
            lon = point.longitude(),
            lat = point.latitude(),
            provider = "ReplayRoute",
            time = eventTimestamp,
            altitude = null,
            accuracyHorizontal = null,
            bearing = null,
            speed = speedMps,
        ),
    )

    private fun verifySkipToIndex(
        pushedEvents: List<ReplayEventBase>,
        primaryRoute: NavigationRoute,
        skipIndex: Int,
    ) {
        val firstReplayEvent = pushedEvents.first() as ReplayEventUpdateLocation
        val firstReplayPoint = Point.fromLngLat(
            firstReplayEvent.location.lon,
            firstReplayEvent.location.lat,
        )
        val geometry = primaryRoute.directionsRoute.geometry()!!
        val expected = PolylineUtils.decode(geometry, 6)[skipIndex]
        assertEquals(expected, firstReplayPoint)
    }

    private fun mockActiveRoutesUpdatedResult(
        geometry: String = TEST_ROUTE_GEOMETRY,
    ): RoutesUpdatedResult = mockk {
        every { navigationRoutes } returns listOf(mockNavigationRoute(geometry))
    }

    private fun mockRouteProgress(): RouteProgress = mockk {
        every { navigationRoute } returns mockNavigationRoute()
        every { currentRouteGeometryIndex } returns 0
    }

    private val testRouteVertices by lazy { PolylineUtils.decode(TEST_ROUTE_GEOMETRY, 6) }

    /** A route that follows the test route until [index], then leaves it for good. */
    private fun routeDivergingAtVertex(index: Int): String {
        val points = testRouteVertices.toMutableList()
        for (i in index until points.size) {
            points[i] = Point.fromLngLat(
                points[i].longitude() + 0.00005,
                points[i].latitude() + 0.00005,
            )
        }
        return PolylineUtils.encode(points, 6)
    }

    /**
     * Leaves [TEST_ROUTE_GEOMETRY] right after its first vertex, so that a change to it is a
     * change to the road ahead wherever the driver is.
     */
    private val divergingRouteGeometry: String by lazy { routeDivergingAtVertex(1) }

    /** A point on the test route, partway between vertex [index] and the one after it. */
    private fun pointAfterVertex(index: Int): Point =
        TurfMeasurement.midpoint(testRouteVertices[index], testRouteVertices[index + 1])

    private fun mockNavigationRoute(
        geometry: String = TEST_ROUTE_GEOMETRY,
    ): NavigationRoute = mockk {
        every { id } returns "test-navigation-route-id"
        every { directionsRoute } returns mockk {
            every { routeOptions() } returns mockk {
                every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE6
            }
            every { geometry() } returns geometry
        }
    }

    private companion object {
        private const val TEST_ROUTE_GEOMETRY =
            "_kmbgAppafhFwXaOuC}ApAoEbNqe@jAaEhEcOtAwEdAoD`DaLnCiJ|M_e@`Je[rAyEnEgO" +
                "tGiUxByHlDjBp@^zKdG`Ah@`HtDx@d@rGlDl@\\pAp@dAl@p@^nItEpQvJfAh@fDjB`D`Br@`@" +
                "nKpFbDhB~KlGtDvBvAwE|EqPzFeSvHaXtA{ElAiE|@_D"
    }
}
