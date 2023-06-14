package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesSetStartedParams
import com.mapbox.navigation.core.directions.session.SetNavigationRoutesStartedObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createBearing
import com.mapbox.navigation.testing.factories.createCoordinatesList
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createWaypoint
import com.mapbox.navigation.utils.internal.toPoint
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

internal class OnlineRouteAlternativesSwitchTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    @Test
    fun `nothing happens for online routes`() = runBlockingTest {
        val testRoutes = createTestRoutes(
            routerOrigin = RouterOrigin.Offboard
        )
        val testMapboxNavigation = TestMapboxNavigation()
        var routeRequestCount = 0

        val onlineRoutesDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                testMapboxNavigation.routeProgressEvents,
                routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                routeRequestMechanism = { options ->
                    routeRequestCount++
                    createOnlineRoute(options)
                }
            ).toList()
        }
        testMapboxNavigation.apply {
            setRoutes(testRoutes)
            destroy()
        }

        val onlineRoutes = onlineRoutesDeferred.await()
        assertEquals(0, onlineRoutes.size)
        assertEquals(0, routeRequestCount)
    }

    @Test
    fun `online route is calculated for an offline route`() = runBlockingTest {
        val testRoutes = createTestRoutes(
            routerOrigin = RouterOrigin.Onboard,
            testCoordinates = listOf(
                Point.fromLngLat(1.1, 1.1),
                Point.fromLngLat(2.2, 2.2),
            ),
            bearings = listOf(
                createBearing(angle = 99.0),
                null
            ),
            avoidManeuverRadius = null
        )
        val testMapboxNavigation = TestMapboxNavigation()
        val newLocation = createLocation(
            longitudeValue = 33.0,
            latitudeValue = 99.0,
            bearingValue = 250.0f,
            speedValue = 8.0f
        )

        val onlineRoutesEventsDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                createRouteProgress(testRoutes.first()).toStateFlow(),
                newLocation.toStateFlow(),
                routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                avoidManeuverSeconds = 8
            ).toList()
        }
        testMapboxNavigation.apply {
            setRoutes(testRoutes)
            destroy()
        }
        val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

        assertEquals(1, onlineRoutesEvents.size)
        val onlinePrimaryRoute = onlineRoutesEvents.first().first()
        assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
        assertEquals(
            newLocation.enhancedLocation.toPoint(),
            onlinePrimaryRoute.routeOptions.coordinatesList().first()
        )
        assertEquals(
            newLocation.enhancedLocation.bearing.toDouble(),
            onlinePrimaryRoute.routeOptions.bearingsList()?.first()?.angle()
        )
        assertEquals(
            64.0,
            onlinePrimaryRoute.routeOptions.avoidManeuverRadius()
        )
    }

    @Test
    fun `online route is not calculated for an offline route in case of internal calculation failure`() =
        runBlockingTest(
            Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
        ) {
            val testRoutes = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard
            )
            val routeProgressFlow = createRouteProgress(
                testRoutes.first(),
                // incorrect reaming waypoints value should cause internal failure
                remainingWaypointsValue = -1,
            ).toStateFlow()
            val testMapboxNavigation = TestMapboxNavigation()
            var requestsCount = 0

            val onlineRoutesEventDeferred = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    routeProgressFlow,
                    createLocation().toStateFlow(),
                    routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = { options ->
                        requestsCount++
                        createOnlineRoute(options)
                    },
                    minimumRetryInterval = 100,
                ).toList()
            }
            testMapboxNavigation.apply {
                setRoutes(testRoutes)
            }
            assertTrue(onlineRoutesEventDeferred.isActive)
            advanceTimeBy(200)
            assertTrue(onlineRoutesEventDeferred.isActive)
            routeProgressFlow.value = createRouteProgress(testRoutes.first())
            testMapboxNavigation.destroy()
            val onlineRoutesEvents = onlineRoutesEventDeferred.await()

            assertEquals(1, requestsCount)
            assertEquals(1, onlineRoutesEvents.size)
        }

    @Test
    fun `online route is calculated for an offline route on second leg`() = runBlockingTest {
        val testCoordinates = listOf(
            Point.fromLngLat(1.1, 1.1),
            Point.fromLngLat(2.2, 2.2),
            Point.fromLngLat(3.3, 3.3),
        )
        val testRoutes = createTestRoutes(
            testCoordinates,
            RouterOrigin.Onboard,

        )
        val newLocation = createLocation(
            longitudeValue = 2.5,
            latitudeValue = 2.5,
            bearingValue = 30.0f,
            speedValue = 3f
        )
        val routeProgress = createRouteProgress(
            testRoutes.first(),
            remainingWaypointsValue = 1,
        )
        val testMapboxNavigation = TestMapboxNavigation()

        val onlineRoutesEventsDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                routeProgress.toStateFlow(),
                newLocation.toStateFlow(),
                routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                avoidManeuverSeconds = 4,
            ).toList()
        }
        testMapboxNavigation.apply {
            setRoutes(testRoutes)
            destroy()
        }
        val onlineRoutesEvents = onlineRoutesEventsDeferred.await()
        assertEquals(1, onlineRoutesEvents.size)
        val onlinePrimaryRoute = onlineRoutesEvents.first().first()
        assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
        assertEquals(
            listOf(
                newLocation.enhancedLocation.toPoint(),
                testRoutes.first().routeOptions.coordinatesList().last()
            ),
            onlinePrimaryRoute.routeOptions.coordinatesList()
        )
        assertEquals(
            listOf(
                newLocation.enhancedLocation.bearing.toDouble(),
                null
            ),
            onlinePrimaryRoute.routeOptions.bearingsList()?.map { it?.angle() }
        )
        assertEquals(
            12.0,
            onlinePrimaryRoute.routeOptions.avoidManeuverRadius(),
        )
    }

    @Test
    fun `offline route is cleaned up before online is calculated`() = runBlockingTest {
        val offlineRoutes = createNavigationRoutes(
            routerOrigin = RouterOrigin.Onboard,
        )
        val onlineRouteCalculated = CompletableDeferred<Unit>()
        val testMapboxNavigation = TestMapboxNavigation()
        val onlineRoutesDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                createRouteProgress(offlineRoutes.first()).toStateFlow(),
                routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                routeRequestMechanism = { options ->
                    onlineRouteCalculated.await()
                    createOnlineRoute(options)
                },
            ).toList()
        }

        assertTrue(onlineRoutesDeferred.isActive)
        testMapboxNavigation.apply {
            setRoutes(offlineRoutes)
            setRoutes(emptyList())
        }
        onlineRouteCalculated.complete(Unit)
        testMapboxNavigation.destroy()

        val onlineRoutes = onlineRoutesDeferred.await()
        assertEquals(0, onlineRoutes.size)
    }

    @Test
    fun `online route is calculated after routes are cleaned up but before cleanup is processed`() =
        runBlockingTest {
            val offlineRoute = createNavigationRoutes(
                routerOrigin = RouterOrigin.Onboard,
            )
            val onlineRouteCalculated = CompletableDeferred<Unit>()
            val testMapboxNavigation = TestMapboxNavigation()
            val cleanupProcessing = CompletableDeferred<Unit>()
            val onlineRoutesCollection = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    createRouteProgress(offlineRoute.first()).toStateFlow(),
                    routesSetStartedEvents = testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = { options ->
                        onlineRouteCalculated.await()
                        createOnlineRoute(options)
                    },
                ).toList()
            }

            assertTrue(onlineRoutesCollection.isActive)
            testMapboxNavigation.apply {
                setRoutes(offlineRoute)
                setRoutes(
                    emptyList(),
                    routesProcessing = cleanupProcessing
                )
            }
            onlineRouteCalculated.complete(Unit)
            cleanupProcessing.complete(Unit)

            testMapboxNavigation.destroy()

            onlineRoutesCollection.cancel()
            val collectedOnlineRoutes = onlineRoutesCollection.await()
            assertEquals(0, collectedOnlineRoutes.size)
        }

    @Test
    fun `offline route is updated during online route calculation`() = runBlockingTest {
        val currentPosition = Point.fromLngLat(1.0, 1.0)
        val firstOfflineRoutes = createNavigationRoutes(
            routerOrigin = RouterOrigin.Onboard,
            options = createRouteOptions(
                coordinatesList = listOf(
                    currentPosition,
                    Point.fromLngLat(2.0, 2.0)
                )
            )
        )
        val secondOfflineRoutes = createNavigationRoutes(
            routerOrigin = RouterOrigin.Onboard,
            options = createRouteOptions(
                coordinatesList = listOf(
                    currentPosition,
                    Point.fromLngLat(3.0, 3.0)
                )
            )
        )
        val newLocation = createLocation(
            latitudeValue = currentPosition.latitude(),
            longitudeValue = currentPosition.longitude()
        )
        val onlineRouteRequestWaitHandle = CompletableDeferred<Unit>()
        val testMapboxNavigation = TestMapboxNavigation()

        val onlineRoutesEventsDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                testMapboxNavigation.routeProgressEvents,
                newLocation.toStateFlow(),
                testMapboxNavigation.flowSetNavigationRoutesStarted,
                routeRequestMechanism = { options ->
                    onlineRouteRequestWaitHandle.await()
                    createOnlineRoute(options)
                }
            ).toList()
        }
        testMapboxNavigation.apply {
            setRoutes(firstOfflineRoutes)
            setRoutes(secondOfflineRoutes)
            onlineRouteRequestWaitHandle.complete(Unit)
            destroy()
        }
        val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

        assertEquals(1, onlineRoutesEvents.size)
        val onlineRoutes = onlineRoutesEvents.first()
        assertEquals(
            secondOfflineRoutes.first().routeOptions.coordinatesList(),
            onlineRoutes.first().routeOptions.coordinatesList()
        )
    }

    @Test
    fun `retry calculating online route with delay in case of immediate failure`() =
        runBlockingTest(
            Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
        ) {
            val testRoutes = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard,
                testCoordinates = listOf(
                    Point.fromLngLat(1.1, 1.1),
                    Point.fromLngLat(2.2, 2.2),
                )
            )
            val locationUpdatesValue = listOf(
                createLocation(latitudeValue = 1.1, longitudeValue = 1.1),
                createLocation(latitudeValue = 1.2, longitudeValue = 1.1),
                createLocation(latitudeValue = 1.3, longitudeValue = 1.1),
            )
            val locationUpdatesFlow = locationUpdatesValue.first().toStateFlow()
            var routeRequestCount = 0
            val routeRequest: RouteRequestMechanism = { options ->
                routeRequestCount++
                if (options.coordinatesList().first().latitude() == 1.3) {
                    createOnlineRoute(options)
                } else {
                    DirectionsRequestResult.ErrorResponse.RetryableError
                }
            }
            val testMapboxNavigation = TestMapboxNavigation()

            val onlineRoutesEventsDeferred = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    createRouteProgress(testRoutes.first()).toStateFlow(),
                    locationUpdatesFlow,
                    testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = routeRequest,
                    minimumRetryInterval = 100
                ).toList()
            }
            testMapboxNavigation.apply {
                setRoutes(testRoutes)
            }
            assertTrue(onlineRoutesEventsDeferred.isActive)
            assertEquals(1, routeRequestCount)
            advanceTimeBy(500)
            assertEquals(6, routeRequestCount)
            locationUpdatesFlow.value = locationUpdatesValue[2]
            testMapboxNavigation.destroy()
            val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

            assertEquals(1, onlineRoutesEvents.size)
            val onlinePrimaryRoute = onlineRoutesEvents.first().first()
            assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
            assertEquals(
                locationUpdatesValue[2].enhancedLocation.toPoint(),
                onlinePrimaryRoute.routeOptions.coordinatesList().first()
            )
        }

    @Test
    fun `retry calculating online route in case of an exception from route request mechanism`() =
        runBlockingTest(
            Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
        ) {
            val testRoutes = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard,
                testCoordinates = listOf(
                    Point.fromLngLat(1.1, 1.1),
                    Point.fromLngLat(2.2, 2.2),
                )
            )
            val locationUpdatesValue = listOf(
                createLocation(latitudeValue = 1.1, longitudeValue = 1.1),
                createLocation(latitudeValue = 1.3, longitudeValue = 1.1),
            )
            val locationUpdatesFlow = locationUpdatesValue.first().toStateFlow()
            val routeRequest: RouteRequestMechanism = { options ->
                if (options.coordinatesList().first().latitude() == 1.3) {
                    createOnlineRoute(options)
                } else {
                    error("test error")
                }
            }
            val testMapboxNavigation = TestMapboxNavigation()

            val onlineRoutesEventsDeferred = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    createRouteProgress(testRoutes.first()).toStateFlow(),
                    locationUpdatesFlow,
                    testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = routeRequest,
                    minimumRetryInterval = 100
                ).toList()
            }
            testMapboxNavigation.apply {
                setRoutes(testRoutes)
            }
            assertTrue(onlineRoutesEventsDeferred.isActive)
            locationUpdatesFlow.value = locationUpdatesValue[1]
            testMapboxNavigation.destroy()
            val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

            assertEquals(1, onlineRoutesEvents.size)
            val onlinePrimaryRoute = onlineRoutesEvents.first().first()
            assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
        }

    @Test
    fun `retry calculating online route with connection delay`() = runBlockingTest(
        Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
    ) {
        val testRoutes = createTestRoutes(
            routerOrigin = RouterOrigin.Onboard,
            testCoordinates = listOf(
                Point.fromLngLat(1.1, 1.1),
                Point.fromLngLat(2.2, 2.2),
            )
        )
        val locationUpdatesValue = listOf(
            createLocation(latitudeValue = 1.1, longitudeValue = 1.1),
            createLocation(latitudeValue = 1.2, longitudeValue = 1.1),
            createLocation(latitudeValue = 1.3, longitudeValue = 1.1),
        )
        val locationUpdatesFlow = locationUpdatesValue.first().toStateFlow()
        var routeRequestCount = 0
        val routeRequest: RouteRequestMechanism = { options ->
            delay(500)
            routeRequestCount++
            if (options.coordinatesList().first().latitude() == 1.3) {
                createOnlineRoute(options)
            } else {
                DirectionsRequestResult.ErrorResponse.RetryableError
            }
        }
        val testMapboxNavigation = TestMapboxNavigation()

        val onlineRoutesEventsDeferred = async {
            requestOnlineRoutesTestWrapperWithDefaultValues(
                testMapboxNavigation.flowRoutesUpdated,
                createRouteProgress(testRoutes.first()).toStateFlow(),
                locationUpdatesFlow,
                testMapboxNavigation.flowSetNavigationRoutesStarted,
                routeRequestMechanism = routeRequest,
                minimumRetryInterval = 100
            ).toList()
        }
        testMapboxNavigation.apply {
            setRoutes(testRoutes)
        }
        assertTrue(onlineRoutesEventsDeferred.isActive)
        advanceTimeBy(1001)
        assertEquals(2, routeRequestCount)
        locationUpdatesFlow.value = locationUpdatesValue[2]
        testMapboxNavigation.destroy()
        val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

        assertEquals(1, onlineRoutesEvents.size)
        val onlinePrimaryRoute = onlineRoutesEvents.first().first()
        assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
    }

    @Test
    fun `not retryable error stops retries`() =
        runBlockingTest(
            Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
        ) {
            val brokenDestination = Point.fromLngLat(2.2, 2.2)
            val offlineRouteWhereOnlineCanNotBeCalculated = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard,
                testCoordinates = listOf(
                    Point.fromLngLat(1.1, 1.1),
                    brokenDestination,
                )
            )
            val normalDestination = Point.fromLngLat(3.3, 3.3)
            val offlineRoute = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard,
                testCoordinates = listOf(
                    Point.fromLngLat(1.1, 1.1),
                    normalDestination,
                )
            )
            val locationUpdatesFlow = createLocation().toStateFlow()
            var routeRequestCount = 0
            val routeRequest: RouteRequestMechanism = { options ->
                routeRequestCount++
                val destination = options.coordinatesList().last()
                when (destination) {
                    normalDestination -> createOnlineRoute(options)
                    brokenDestination -> DirectionsRequestResult.ErrorResponse.NotRetryableError
                    else -> {
                        fail("not expected destination $destination")
                        error("not expected destination $destination")
                    }
                }
            }
            val testMapboxNavigation = TestMapboxNavigation()

            val onlineRoutesEventsDeferred = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    testMapboxNavigation.routeProgressEvents,
                    locationUpdatesFlow,
                    testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = routeRequest,
                    minimumRetryInterval = 100
                ).toList()
            }
            testMapboxNavigation.apply {
                setRoutes(offlineRouteWhereOnlineCanNotBeCalculated)
            }
            assertTrue(onlineRoutesEventsDeferred.isActive)
            assertEquals(1, routeRequestCount)
            advanceTimeBy(500)
            assertEquals(1, routeRequestCount)
            testMapboxNavigation.apply {
                setRoutes(offlineRoute)
                testMapboxNavigation.destroy()
            }
            val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

            assertEquals(1, onlineRoutesEvents.size)
            val onlinePrimaryRoute = onlineRoutesEvents.first().first()
            assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
            assertEquals(
                normalDestination,
                onlinePrimaryRoute.routeOptions.coordinatesList().last()
            )
        }

    @Test
    fun `error that requires delay ignores user set intervals`() =
        runBlockingTest(
            Job() // https://github.com/Kotlin/kotlinx.coroutines/issues/1910
        ) {
            val offlineRoute = createTestRoutes(
                routerOrigin = RouterOrigin.Onboard,
            )
            val locationUpdatesFlow = createLocation().toStateFlow()
            var routeRequestCount = 0
            var returnErrorWithRetryDelay = true
            val testRetryDelay = 5_000L
            val routeRequest: RouteRequestMechanism = { options ->
                routeRequestCount++
                if (returnErrorWithRetryDelay) {
                    DirectionsRequestResult.ErrorResponse.RetryableErrorWithDelay(testRetryDelay)
                } else {
                    createOnlineRoute(options)
                }
            }
            val testMapboxNavigation = TestMapboxNavigation()

            val onlineRoutesEventsDeferred = async {
                requestOnlineRoutesTestWrapperWithDefaultValues(
                    testMapboxNavigation.flowRoutesUpdated,
                    testMapboxNavigation.routeProgressEvents,
                    locationUpdatesFlow,
                    testMapboxNavigation.flowSetNavigationRoutesStarted,
                    routeRequestMechanism = routeRequest,
                    minimumRetryInterval = 100
                ).toList()
            }
            testMapboxNavigation.apply {
                setRoutes(offlineRoute)
            }
            assertTrue(onlineRoutesEventsDeferred.isActive)
            assertEquals(1, routeRequestCount)
            advanceTimeBy(3_000)
            assertEquals(1, routeRequestCount)
            advanceTimeBy(testRetryDelay)
            assertEquals(2, routeRequestCount)
            returnErrorWithRetryDelay = false
            advanceTimeBy(testRetryDelay)
            assertEquals(3, routeRequestCount)

            testMapboxNavigation.destroy()
            val onlineRoutesEvents = onlineRoutesEventsDeferred.await()

            assertEquals(1, onlineRoutesEvents.size)
            val onlinePrimaryRoute = onlineRoutesEvents.first().first()
            assertEquals(RouterOrigin.Offboard, onlinePrimaryRoute.origin)
        }
}

private fun createTestRoutes(
    testCoordinates: List<Point> = createCoordinatesList(2),
    routerOrigin: RouterOrigin,
    bearings: List<Bearing?>? = null,
    avoidManeuverRadius: Double? = null
) =
    createNavigationRoutes(
        routerOrigin = routerOrigin,
        response = createDirectionsResponse(
            responseWaypoints = testCoordinates.map {
                createWaypoint(location = it.coordinates().toDoubleArray())
            }
        ),
        options = createRouteOptions(
            coordinatesList = testCoordinates,
            bearingList = bearings,
            avoidManeuverRadius = avoidManeuverRadius
        ),
        waypointsMapper = { _ ->
            testCoordinates.mapIndexed { index, coordinate ->
                Waypoint(
                    "waypoint_$index",
                    coordinate,
                    null,
                    null,
                    null,
                    WaypointType.REGULAR,
                )
            }
        }
    )

private fun createLocation(
    latitudeValue: Double = 8.0,
    longitudeValue: Double = 8.0,
    bearingValue: Float = 8.0f,
    speedValue: Float = 10.0f
) = mockk<LocationMatcherResult>(relaxed = true) {
    every { enhancedLocation } returns mockk(relaxed = true) {
        every { longitude } returns longitudeValue
        every { latitude } returns latitudeValue
        every { bearing } returns bearingValue
        every { speed } returns speedValue
    }
}

private fun createRouteProgress(
    primaryRoute: NavigationRoute,
    remainingWaypointsValue: Int = 1,
): RouteProgress = mockk(relaxed = true) {
    every { navigationRoute } returns primaryRoute
    every { remainingWaypoints } returns remainingWaypointsValue
}

fun <T> T.toStateFlow(): MutableStateFlow<T> = MutableStateFlow(this)

internal suspend fun createOnlineRoute(routeOptions: RouteOptions): DirectionsRequestResult {
    return DirectionsRequestResult.SuccessfulResponse(
        createDirectionsResponse(routeOptions = routeOptions)
    )
}

// wrapper helps adding new parameters without rewriting every call in tests
private fun requestOnlineRoutesTestWrapperWithDefaultValues(
    routesUpdatedEvents: Flow<List<NavigationRoute>>,
    routeProgressUpdate: Flow<RouteProgress>,
    locationUpdates: Flow<LocationMatcherResult> = createLocation().toStateFlow(),
    routesSetStartedEvents: Flow<RoutesSetStartedParams>,
    routeRequestMechanism: RouteRequestMechanism = ::createOnlineRoute,
    minimumRetryInterval: Long = 1_000,
    avoidManeuverSeconds: Int = 3,
    navigationRouteSerializationDispatcher: CoroutineDispatcher = TestCoroutineDispatcher()
): Flow<List<NavigationRoute>> {
    return requestOnlineRoutes(
        routesUpdatedEvents = routesUpdatedEvents,
        matchingResults = locationUpdates,
        routeProgressUpdates = routeProgressUpdate,
        setNavigaitonRoutesStartedEvents = routesSetStartedEvents,
        routeRequestMechanism = routeRequestMechanism,
        minimumRetryInterval = minimumRetryInterval,
        navigationRouteSerializationDispatcher = navigationRouteSerializationDispatcher,
        avoidManeuverSeconds = avoidManeuverSeconds
    )
}

private class TestMapboxNavigation {

    private val directionsSession = MapboxDirectionsSession(mockk())
    private val onClose = mutableListOf<() -> Unit>()

    val flowRoutesUpdated: Flow<List<NavigationRoute>>
        get() = callbackFlow {
            onClose.add {
                this.close()
            }
            val observer = RoutesObserver { trySend(it) }
            directionsSession.registerSetNavigationRoutesFinishedObserver(observer)
            awaitClose { directionsSession.unregisterSetNavigationRoutesFinishedObserver(observer) }
        }.map { it.navigationRoutes }

    val flowSetNavigationRoutesStarted: Flow<RoutesSetStartedParams>
        get() = callbackFlow {
            onClose.add {
                this.close()
            }
            val observer = SetNavigationRoutesStartedObserver { trySend(it) }
            directionsSession.registerSetNavigationRoutesStartedObserver(observer)
            awaitClose { directionsSession.unregisterSetNavigationRoutesStartedObserver(observer) }
        }

    val routeProgressEvents: Flow<RouteProgress>
        get() = flowRoutesUpdated.mapNotNull {
            if (it.isNotEmpty()) {
                createRouteProgress(primaryRoute = it.first())
            } else {
                null
            }
        }

    fun CoroutineScope.setRoutes(
        routes: List<NavigationRoute>,
        initialLegIndex: Int = 0,
        routesProcessing: Deferred<Unit>? = null
    ) {
        launch {
            directionsSession.setNavigationRoutesStarted(RoutesSetStartedParams(routes))
            routesProcessing?.await()
            val setRoutesParams = if (routes.isEmpty()) {
                SetRoutes.CleanUp
            } else {
                SetRoutes.NewRoutes(initialLegIndex)
            }
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(routes, emptyList(), setRoutesParams)
            )
        }
    }

    fun destroy() {
        onClose.forEach { it.invoke() }
    }
}
