package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CurrentIndices
import com.mapbox.navigation.base.internal.CurrentIndicesFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.core.CurrentIndicesProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.testing.add
import com.mapbox.navigation.testing.factories.createCoordinatesList
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.utcToLocalTime
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.time.Month
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class RouteRefreshControllerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)
    private val currentIndices = CurrentIndicesFactory.createIndices(0, 1, 2)
    private val currentIndicesProvider =
        mockk<CurrentIndicesProvider>(relaxed = true) {
            coEvery { getFilledIndicesOrWait() } returns currentIndices
        }

    @Before
    fun setup() {
        LoggerProvider.setLoggerFrontend(logger)
    }

    @Test
    fun `route with disabled refresh never refreshes`() = runBlockingTest {
        val testRoute = createNavigationRoute(
            createDirectionsRoute(
                routeOptions = createRouteOptions(
                    enableRefresh = false
                )
            )
        )
        val routeRefreshController = createRouteRefreshController()

        val refreshJob = async { routeRefreshController.refresh(listOf(testRoute)) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(3))

        assertTrue(refreshJob.isActive)
        refreshJob.cancel()
    }

    @Test
    fun `route refreshes`() = runBlockingTest {
        val (initialRoute, refreshedRoute) = createTestInitialAndRefreshedTestRoutes()
        val routeRefreshStub = RouteRefreshStub().apply {
            setRefreshedRoute(refreshedRoute)
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefreshStub,
            routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build(),
        )

        val refreshJob = async { routeRefreshController.refresh(listOf(initialRoute)) }
        advanceTimeBy(TimeUnit.SECONDS.toMillis(30))

        assertEquals(
            RefreshedRouteInfo(listOf(refreshedRoute), currentIndices),
            refreshJob.getCompletedTest()
        )
    }

    @Test
    fun `should refresh route with any annotation`() = runBlockingTest {
        val routeWithoutAnnotations = createNavigationRoute(
            createTestTwoLegRoute(
                firstLegAnnotations = null,
                secondLegAnnotations = null
            )
        )
        val refreshedRoute = createNavigationRoute()
        val routeRefreshStub = RouteRefreshStub().apply {
            setRefreshedRoute(refreshedRoute)
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefreshStub
        )

        val refreshedRouteDeferred =
            async { routeRefreshController.refresh(listOf(routeWithoutAnnotations)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertEquals(
            RefreshedRouteInfo(listOf(refreshedRoute), currentIndices),
            refreshedRouteDeferred.getCompletedTest()
        )
    }

    @Test
    fun `should log warning when the only route is not supported`() = runBlockingTest {
        val primaryRoute = createNavigationRoute(createTestTwoLegRoute(requestUuid = null))
        val routeRefreshController = createRouteRefreshController()

        val refreshedDeferred = async { routeRefreshController.refresh(listOf(primaryRoute)) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(6))

        assertTrue(refreshedDeferred.isActive)
        verify(exactly = 1) {
            logger.logI(
                withArg {
                    assertTrue(
                        "message doesn't mention the reason of failure - empty uuid: $it",
                        it.contains("uuid", ignoreCase = true)
                    )
                },
                any()
            )
        }
        refreshedDeferred.cancel()
    }

    @Test
    fun `refreshing of empty routes`() = runBlockingTest {
        val routeRefreshController = createRouteRefreshController()

        val refreshedDeferred = async { routeRefreshController.refresh(listOf()) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(6))

        assertTrue(refreshedDeferred.isActive)
        refreshedDeferred.cancel()
    }

    @Test
    fun `cancel request when stopped`() = runBlockingTest {
        val routeRefresh = mockk<RouteRefresh> {
            every { requestRouteRefresh(any(), any(), any()) } returns 8
            every { cancelRouteRefreshRequest(any()) } returns Unit
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefresh
        )

        val refreshJob =
            async {
                routeRefreshController.refresh(
                    listOf(
                        createNavigationRoute(
                            createTestTwoLegRoute()
                        )
                    )
                )
            }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        refreshJob.cancel()

        verify(exactly = 1) { routeRefresh.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 1) { routeRefresh.cancelRouteRefreshRequest(8) }
    }

    @Test
    fun `do not send a request when uuid is empty`() = runBlockingTest {
        val routeRefresh = mockk<RouteRefresh>(relaxed = true)
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefresh
        )
        val route = createNavigationRoute(createTestTwoLegRoute(requestUuid = ""))

        val refreshDeferred = launch { routeRefreshController.refresh(listOf(route)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertTrue(refreshDeferred.isActive)
        verify(exactly = 0) { routeRefresh.requestRouteRefresh(any(), any(), any()) }
        refreshDeferred.cancel()
    }

    @Test
    fun `clean up of a route without legs never returns`() = runBlockingTest {
        val initialRoute = createNavigationRoute(
            createDirectionsRoute(
                legs = null,
                createRouteOptions(enableRefresh = true)
            )
        )
        val routeRefresh = RouteRefreshStub().apply {
            failRouteRefresh(initialRoute.id)
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefresh
        )

        val result = async { routeRefreshController.refresh(listOf(initialRoute)) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(6))

        assertTrue("route refresh has finished $result", result.isActive)
        result.cancel()
    }

    @Test
    fun `refresh alternative route without legs`() = runBlockingTest {
        val initialRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createTestTwoLegRoute(requestUuid = "test1"),
                    createTestTwoLegRoute(requestUuid = "test2")
                        .toBuilder().legs(emptyList()).build()
                )
            )
        )
        val refreshedRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    initialRoutes.first().directionsRoute,
                    createTestTwoLegRoute(requestUuid = "test2")
                )
            )
        )
        val routeRefresh = RouteRefreshStub().apply {
            setRefreshedRoute(refreshedRoutes[0])
            setRefreshedRoute(refreshedRoutes[1])
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefresh
        )

        val result = routeRefreshController.refresh(initialRoutes)

        assertEquals(RefreshedRouteInfo(refreshedRoutes, currentIndices), result)
    }

    @Test
    fun `should log route diffs when there is a successful response`() =
        runBlockingTest {
            val initialRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    uuid = "test",
                    routes = listOf(
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("moderate", "heavy"),
                                congestionNumeric = listOf(50, 94)
                            ),
                        ),
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("moderate", "heavy"),
                                congestionNumeric = listOf(50, 94)
                            ),
                        )
                    )
                )
            )
            val refreshedRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    uuid = "test",
                    routes = listOf(
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("heavy", "heavy"),
                                congestionNumeric = listOf(93, 94),
                            ),
                            firstLegIncidents = listOf(
                                createIncident(id = "1")
                            )
                        ),
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("moderate", "heavy"),
                                congestionNumeric = listOf(50, 94)
                            ),
                        )
                    )
                )
            )
            val routeRefresh = RouteRefreshStub().apply {
                setRefreshedRoute(refreshedRoutes[0])
                setRefreshedRoute(refreshedRoutes[1])
            }
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = routeRefresh,
            )

            routeRefreshController.refresh(initialRoutes)

            verify {
                logger.logI(
                    "Updated congestion, congestionNumeric, incidents at route test#0 leg 0",
                    RouteRefreshController.LOG_CATEGORY
                )
            }
        }

    @Test
    fun `should log message when there is a successful response without route diffs`() =
        runBlockingTest {
            val initialRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(createTestTwoLegRoute()),
                    uuid = "testNoDiff"
                )
            )
            val refreshedRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(createTestTwoLegRoute()),
                    uuid = "testNoDiff"
                )
            )
            val routeRefreshStub = RouteRefreshStub().apply {
                setRefreshedRoute(refreshedRoutes[0])
            }
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = routeRefreshStub,
            )

            val refreshJob = launch { routeRefreshController.refresh(initialRoutes) }
            advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
            refreshJob.cancel()

            verify {
                logger.logI(
                    withArg {
                        it.contains("no changes", ignoreCase = true) &&
                            it.contains("testNoDiff#0")
                    },
                    RouteRefreshController.LOG_CATEGORY
                )
            }
        }

    @Test
    fun `traffic annotations and incidents on all legs starting with the current(first) disappears if refresh fails`() =
        runBlockingTest {
            val currentTime = utcToLocalTime(
                year = 2022,
                month = Month.MAY,
                date = 22,
                hourOfDay = 12,
                minute = 30,
                second = 0
            )
            val primaryRoute = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegIncidents = listOf(
                        createIncident(
                            id = "1",
                            endTime = "2022-05-22T14:00:00Z",
                        ),
                        createIncident(
                            id = "2",
                            endTime = "2022-05-22T12:00:00Z", // expired
                        ),
                        createIncident(
                            id = "3",
                            endTime = "2022-05-22T12:00:00-01",
                        ),
                        createIncident(
                            id = "4",
                            endTime = null,
                        )
                    ),
                    secondLegIncidents = listOf(
                        createIncident(
                            id = "5",
                            endTime = "2022-05-23T10:00:00Z",
                        ),
                        createIncident(
                            id = "6",
                            endTime = "2022-05-22T12:29:00Z", // expired
                        ),
                        createIncident(
                            id = "7",
                            endTime = "wrong date format",
                        )
                    )
                )
            )
            val routeRefreshStub = RouteRefreshStub().apply {
                failRouteRefresh(primaryRoute.id)
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                localDateProvider = { currentTime },
                routeRefresh = routeRefreshStub,
            )
            // act
            val refreshedRoutesDeffer = async {
                routeRefreshController.refresh(listOf(primaryRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            // assert
            val refreshedRoute = refreshedRoutesDeffer.getCompletedTest().routes.first()
            refreshedRoute.assertCongestionExpiredForLeg(0)
            refreshedRoute.assertCongestionExpiredForLeg(1)
            assertEquals(
                listOf("1", "3", "4"),
                refreshedRoute.directionsRoute.legs()!![0].incidents()?.map { it.id() }
            )
            assertEquals(
                listOf("5", "7"),
                refreshedRoute.directionsRoute.legs()!![1].incidents()?.map { it.id() }
            )
        }

    @Test
    fun `after invalidation route isn't updated until successful refresh`() =
        runBlockingTest {
            val initialRoute = createNavigationRoute(createTestTwoLegRoute())
            val routeRefreshStub = RouteRefreshStub().apply {
                failRouteRefresh(initialRoute.id)
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeRefresh = routeRefreshStub
            )
            val invalidatedRouteDeffer = async {
                routeRefreshController.refresh(listOf(initialRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            val invalidatedRoute = invalidatedRouteDeffer.getCompletedTest().routes.first()
            // act
            val refreshedRoute = async {
                routeRefreshController.refresh(listOf(invalidatedRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis) * 100
            )
            assertFalse(refreshedRoute.isCompleted)
            routeRefreshStub.setRefreshedRoute(initialRoute)
            advanceTimeBy(routeRefreshOptions.intervalMillis)
            // assert
            assertEquals(
                RefreshedRouteInfo(listOf(initialRoute), currentIndices),
                refreshedRoute.getCompletedTest()
            )
        }

    @Test
    fun `after invalidation route isn't updated until incident expiration`() =
        runBlockingTest {
            var currentTime = utcToLocalTime(
                year = 2022,
                month = Month.MAY,
                date = 22,
                hourOfDay = 9,
                minute = 0,
                second = 0
            )
            val initialRoute = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegIncidents = listOf(
                        createIncident(
                            id = "1",
                            endTime = "2022-05-22T12:00:00Z" // expires in 3 hours
                        )
                    )
                )
            )
            val routeRefreshStub = RouteRefreshStub().apply {
                failRouteRefresh(initialRoute.id)
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeRefresh = routeRefreshStub,
                localDateProvider = { currentTime }
            )
            val invalidatedRouteDeffer = async {
                routeRefreshController.refresh(listOf(initialRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            val invalidatedRoute = invalidatedRouteDeffer.getCompletedTest().routes.first()
            // act
            val refreshedRoute = async {
                routeRefreshController.refresh(listOf(invalidatedRoute))
            }
            val twoHours = TimeUnit.HOURS.toMillis(2)
            currentTime = currentTime.add(milliseconds = twoHours)
            advanceTimeBy(twoHours)
            assertFalse("incident should not expire in 2 hours", refreshedRoute.isCompleted)
            val oneHour = TimeUnit.HOURS.toMillis(1)
            currentTime = currentTime.add(milliseconds = oneHour)
            advanceTimeBy(oneHour)
            // assert
            assertEquals(
                emptyList<Incident>(),
                refreshedRoute.getCompletedTest().routes.first()
                    .directionsResponse.routes().first().legs()?.first()?.incidents()
            )
        }

    @Test
    fun `traffic annotations and expired annotations on current leg(second) disappear if refresh doesn't respond`() =
        runBlockingTest {
            val currentTime = utcToLocalTime(
                year = 2022,
                month = Month.MAY,
                date = 22,
                hourOfDay = 10,
                minute = 0,
                second = 0
            )
            val currentRoute = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegIncidents = listOf(
                        createIncident(
                            id = "1",
                            endTime = "2022-05-22T09:00:00Z",
                        ),
                    ),
                    secondLegIncidents = listOf(
                        createIncident(
                            id = "2",
                            endTime = "2022-05-22T09:59:00Z"
                        ),
                        createIncident(
                            id = "3",
                            endTime = "2022-05-22T10:00:01Z"
                        ),
                    )
                )
            )
            val routeRefreshStub = RouteRefreshStub().apply {
                doNotRespondForRouteRefresh(currentRoute.id)
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000L)
                .build()
            coEvery {
                currentIndicesProvider.getFilledIndicesOrWait()
            } returns CurrentIndicesFactory.createIndices(1, 0, null)
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeDiffProvider = DirectionsRouteDiffProvider(),
                localDateProvider = { currentTime },
                routeRefresh = routeRefreshStub
            )
            // act
            val refreshedRoutesDeferred = async {
                routeRefreshController.refresh(listOf(currentRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestionsInCaseOfTimeout(
                    routeRefreshOptions.intervalMillis
                )
            )
            // assert
            val refreshedRoute = refreshedRoutesDeferred.getCompletedTest().routes.first()
            refreshedRoute.assertCongestionExpiredForLeg(1)
            assertEquals(
                listOf("3"),
                refreshedRoute.directionsRoute.legs()!![1].incidents()?.map { it.id() }
            )
            assertEquals(
                "annotations on passed legs should not be refreshed",
                currentRoute.directionsRoute.legs()!![0].annotation(),
                refreshedRoute.directionsRoute.legs()!![0].annotation()!!
            )
            assertEquals(
                "incidents on passed legs should not be refreshed",
                currentRoute.directionsRoute.legs()!![0].incidents(),
                refreshedRoute.directionsRoute.legs()!![0].incidents()!!
            )
        }

    @Test
    fun `route successfully refreshes on time if first try doesn't respond`() =
        runBlockingTest {
            val initialRoute = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegAnnotations = createRouteLegAnnotation(
                        congestion = listOf("severe", "moderate"),
                        congestionNumeric = listOf(90, 50),
                    )
                )
            )
            val refreshed = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegAnnotations = createRouteLegAnnotation(
                        congestion = listOf("severe", "severe"),
                        congestionNumeric = listOf(90, 90),
                    )
                )
            )
            var currentRoute = initialRoute
            val directionsSession = mockk<DirectionsSession>(relaxed = true)
                .onRefresh { refreshAttempt, _, _, callback ->
                    if (refreshAttempt >= 1) {
                        callback.onRefreshReady(currentRoute)
                    }
                }
            val refreshInterval = 30_000L
            val refreshController = createRouteRefreshController(
                routeRefresh = directionsSession,
                routeRefreshOptions = RouteRefreshOptions.Builder()
                    .intervalMillis(refreshInterval)
                    .build()
            )

            val refreshedDeferred = async { refreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(refreshInterval)
            assertFalse(refreshedDeferred.isCompleted)
            currentRoute = refreshed
            advanceTimeBy(refreshInterval)

            assertEquals(
                RefreshedRouteInfo(listOf(refreshed), currentIndices),
                refreshedDeferred.getCompletedTest()
            )
        }

    @Test
    fun `route successfully refreshes on time if first try failed`() =
        runBlockingTest {
            val initialRoute = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegAnnotations = createRouteLegAnnotation(
                        congestion = listOf("severe", "moderate"),
                        congestionNumeric = listOf(90, 50),
                    )
                )
            )
            val refreshed = createNavigationRoute(
                createTestTwoLegRoute(
                    firstLegAnnotations = createRouteLegAnnotation(
                        congestion = listOf("severe", "severe"),
                        congestionNumeric = listOf(90, 90),
                    )
                )
            )
            var currentRoute = initialRoute

            val directionsSession = mockk<DirectionsSession>(relaxed = true)
                .onRefresh { refreshAttempt, _, _, callback ->
                    if (refreshAttempt >= 1) {
                        callback.onRefreshReady(currentRoute)
                    } else {
                        callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
                    }
                }
            val refreshInterval = 30_000L
            val refreshController = createRouteRefreshController(
                routeRefresh = directionsSession,
                routeRefreshOptions = RouteRefreshOptions.Builder()
                    .intervalMillis(refreshInterval)
                    .build()
            )

            val refreshedDeferred = async { refreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(refreshInterval)
            assertFalse(refreshedDeferred.isCompleted)
            currentRoute = refreshed
            advanceTimeBy(refreshInterval)

            assertEquals(
                RefreshedRouteInfo(listOf(refreshed), currentIndices),
                refreshedDeferred.getCompletedTest()
            )
        }

    @Test
    fun `successful refresh of two routes`() = runBlockingTest {
        val initialRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "moderate"),
                            congestionNumeric = listOf(90, 50),
                        )
                    ),
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "moderate"),
                            congestionNumeric = listOf(90, 50),
                        )
                    )
                )
            )
        )
        val refreshedRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "severe"),
                            congestionNumeric = listOf(90, 90),
                        )
                    ),
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "severe"),
                            congestionNumeric = listOf(90, 90),
                        )
                    )
                )
            )
        )
        val routeRefreshStub = RouteRefreshStub().apply {
            setRefreshedRoute(refreshedRoutes[0])
            setRefreshedRoute(refreshedRoutes[1])
        }
        val refreshOptions = RouteRefreshOptions.Builder().build()
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefreshStub,
            routeRefreshOptions = refreshOptions
        )

        val refreshedRoutesDeferred = async {
            routeRefreshController.refresh(initialRoutes)
        }
        advanceTimeBy(refreshOptions.intervalMillis)

        val result = refreshedRoutesDeferred.getCompletedTest()

        assertEquals(
            RefreshedRouteInfo(refreshedRoutes, currentIndices),
            result
        )
    }

    @Test
    fun `primary route refresh failed, alternative route refreshed successfully`() =
        runBlockingTest {
            val initialRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("severe", "moderate"),
                                congestionNumeric = listOf(90, 50),
                            )
                        ),
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("severe", "moderate"),
                                congestionNumeric = listOf(90, 50),
                            )
                        )
                    )
                )
            )
            val expectedRefreshedRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(
                        initialRoutes[0].directionsRoute,
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("severe", "severe"),
                                congestionNumeric = listOf(90, 90),
                            )
                        )
                    )
                )
            )
            val routeRefreshStub = RouteRefreshStub().apply {
                failRouteRefresh(expectedRefreshedRoutes[0].id)
                setRefreshedRoute(expectedRefreshedRoutes[1])
            }
            val refreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = routeRefreshStub,
                routeRefreshOptions = refreshOptions
            )

            val refreshedRoutesDeferred = async {
                routeRefreshController.refresh(initialRoutes)
            }
            advanceTimeBy(refreshOptions.intervalMillis)
            val result = refreshedRoutesDeferred.getCompletedTest()

            assertEquals(
                RefreshedRouteInfo(expectedRefreshedRoutes, currentIndices),
                result
            )
        }

    @Test
    fun `routes won't refresh until one of them(second) changes`() = runBlockingTest {
        val routeRefreshStub = RouteRefreshStub()
        val initialRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "moderate"),
                            congestionNumeric = listOf(90, 50),
                        )
                    ),
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "moderate"),
                            congestionNumeric = listOf(90, 50),
                        )
                    )
                )
            )
        )
        val refreshedRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    initialRoutes[0].directionsRoute,
                    createTestTwoLegRoute(
                        firstLegAnnotations = createRouteLegAnnotation(
                            congestion = listOf("severe", "severe"),
                            congestionNumeric = listOf(90, 90),
                        )
                    )
                )
            )
        )
        val refreshOptions = RouteRefreshOptions.Builder().build()
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefreshStub,
            routeRefreshOptions = refreshOptions
        )

        val refreshedRoutesDeferred = async {
            routeRefreshController.refresh(initialRoutes)
        }
        routeRefreshStub.setRefreshedRoute(initialRoutes[0])
        routeRefreshStub.setRefreshedRoute(initialRoutes[1])
        advanceTimeBy(TimeUnit.HOURS.toMillis(7))
        assertFalse(refreshedRoutesDeferred.isCompleted)
        routeRefreshStub.setRefreshedRoute(refreshedRoutes[1])
        advanceTimeBy(refreshOptions.intervalMillis)

        val result = refreshedRoutesDeferred.getCompletedTest()

        assertEquals(
            RefreshedRouteInfo(refreshedRoutes, currentIndices),
            result
        )
    }

    @Test
    fun `should updated primary and log warning when only alternative route is not supported`() =
        runBlockingTest {
            val primaryRoute = createNavigationRoute(createTestTwoLegRoute(requestUuid = "testid"))
            val alternativeRoute = createNavigationRoute(createTestTwoLegRoute(requestUuid = null))
            val updatedPrimary = createNavigationRoute(
                createTestTwoLegRoute(
                    requestUuid = "testid",
                    firstLegAnnotations = createRouteLegAnnotation(
                        congestion = listOf("severe", "severe"),
                        congestionNumeric = listOf(95, 93),
                    )
                )
            )
            val routeRefresh = RouteRefreshStub().apply {
                setRefreshedRoute(updatedPrimary)
            }
            val refreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = routeRefresh,
                routeRefreshOptions = refreshOptions
            )

            val refreshedDeferred = async {
                routeRefreshController.refresh(listOf(primaryRoute, alternativeRoute))
            }
            advanceTimeBy(refreshOptions.intervalMillis)
            val result = refreshedDeferred.getCompletedTest()

            assertEquals(
                RefreshedRouteInfo(
                    listOf(updatedPrimary, alternativeRoute),
                    currentIndices
                ),
                result
            )
            verify(exactly = 1) {
                logger.logI(
                    withArg {
                        assertTrue(
                            "message doesn't mention the reason of failure - empty uuid: $it",
                            it.contains("uuid", ignoreCase = true)
                        )
                        assertTrue(
                            "message doesn't mention the route index",
                            it.contains("0", ignoreCase = true)
                        )
                    },
                    any()
                )
            }
        }

    @Test
    fun `no refreshes when all routes disable refresh`() = runBlockingTest {
        val routes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createDirectionsRoute(
                        routeOptions = createRouteOptions(enableRefresh = false)
                    ),
                    createDirectionsRoute(
                        routeOptions = createRouteOptions(enableRefresh = false)
                    ),
                )
            )
        )
        val routeRefresh = RouteRefreshStub()
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = routeRefresh
        )

        val refreshedDeferred = async { routeRefreshController.refresh(routes) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(8))
        assertFalse(refreshedDeferred.isCompleted)
        refreshedDeferred.cancel()
        verify(exactly = 1) {
            logger.logI(
                withArg {
                    assertTrue(
                        "message doesn't mention the reason of failure - enableRefresh=false: $it",
                        it.contains("enableRefresh", ignoreCase = true)
                    )
                    assertTrue(
                        "message doesn't mention the route index",
                        it.contains("0", ignoreCase = true)
                    )
                },
                any()
            )
            logger.logI(
                withArg {
                    assertTrue(
                        "message doesn't mention the reason of failure - enableRefresh=false: $it",
                        it.contains("enableRefresh", ignoreCase = true)
                    )
                    assertTrue(
                        "message doesn't mention the route index",
                        it.contains("1", ignoreCase = true)
                    )
                },
                any()
            )
        }
    }

    @Test
    fun `traffic annotations are cleaned up if all routes refresh fails`() =
        runBlockingTest {
            val initialRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(
                        createTestTwoLegRoute(),
                        createTestTwoLegRoute()
                    )
                )
            )
            val routeRefresh = RouteRefreshStub().apply {
                failRouteRefresh(initialRoutes[0].id)
                failRouteRefresh(initialRoutes[1].id)
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            coEvery {
                currentIndicesProvider.getFilledIndicesOrWait()
            } returns CurrentIndicesFactory.createIndices(1, 0, null)
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeDiffProvider = DirectionsRouteDiffProvider(),
                routeRefresh = routeRefresh
            )
            // act
            val refreshedRoutesDeferred = async {
                routeRefreshController.refresh(initialRoutes)
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            // assert
            val refreshedRoutes = refreshedRoutesDeferred.getCompletedTest().routes
            refreshedRoutes[0].assertCongestionExpiredForLeg(1)
            refreshedRoutes[1].assertCongestionExpiredForLeg(1)
        }

    @Test
    fun `if route refresh works only for one route, the controller updates only one route`() =
        runBlockingTest {
            val currentTime = utcToLocalTime(
                year = 2022,
                month = Month.MAY,
                date = 22,
                hourOfDay = 12,
                minute = 30,
                second = 0
            )
            val routeRefreshStub = RouteRefreshStub()
            val initialRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(
                        createTestTwoLegRoute(),
                        createTestTwoLegRoute(
                            secondLegIncidents = listOf(
                                createIncident(
                                    id = "1",
                                    endTime = "2022-05-21T14:00:00Z", // expired
                                ),
                                createIncident(
                                    id = "2",
                                    endTime = "2022-05-22T14:00:00Z",
                                ),
                            )
                        )
                    )
                )
            )
            val refreshedRoutes = createNavigationRoutes(
                createDirectionsResponse(
                    routes = listOf(
                        createTestTwoLegRoute(
                            firstLegAnnotations = createRouteLegAnnotation(
                                congestion = listOf("severe", "severe"),
                                congestionNumeric = listOf(90, 99),
                            )
                        ),
                        createTestTwoLegRoute()
                    )
                )
            )
            routeRefreshStub.setRefreshedRoute(refreshedRoutes[0])
            routeRefreshStub.doNotRespondForRouteRefresh(refreshedRoutes[1].id)
            val refreshOptions = RouteRefreshOptions.Builder().build()
            coEvery {
                currentIndicesProvider.getFilledIndicesOrWait()
            } returns CurrentIndicesFactory.createIndices(1, 0, null)
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = routeRefreshStub,
                routeRefreshOptions = refreshOptions,
                localDateProvider = { currentTime },
            )

            val refreshedRoutesDeferred = async {
                routeRefreshController.refresh(initialRoutes)
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestionsInCaseOfTimeout(refreshOptions.intervalMillis)
            )

            val result = refreshedRoutesDeferred.getCompletedTest().routes

            assertEquals(
                refreshedRoutes[0],
                result[0]
            )
            assertEquals(
                initialRoutes[1], // no cleanup happens
                result[1]
            )
        }

    private fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions = RouteRefreshOptions.Builder().build(),
        routeRefresh: RouteRefresh = RouteRefreshStub(),
        routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
        localDateProvider: () -> Date = { Date(1653493148247) }
    ) = RouteRefreshController(
        routeRefreshOptions,
        routeRefresh,
        currentIndicesProvider,
        routeDiffProvider,
        localDateProvider
    )
}

private fun createTestTwoLegRoute(
    firstLegIncidents: List<Incident>? = null,
    secondLegIncidents: List<Incident>? = null,
    firstLegAnnotations: LegAnnotation? = createRouteLegAnnotation(
        congestion = listOf("heavy", "heavy"),
        congestionNumeric = listOf(93, 94),
        distance = listOf(23.0, 24.0),
    ),
    secondLegAnnotations: LegAnnotation? = createRouteLegAnnotation(
        congestion = listOf("heavy", "heavy"),
        congestionNumeric = listOf(95, 96),
        distance = listOf(28.0, 29.0)
    ),
    requestUuid: String? = "testUUID"
) =
    createDirectionsRoute(
        legs = listOf(
            createRouteLeg(
                annotation = firstLegAnnotations,
                incidents = firstLegIncidents
            ),
            createRouteLeg(
                annotation = secondLegAnnotations,
                incidents = secondLegIncidents
            )
        ),
        routeOptions = createRouteOptions(
            enableRefresh = true,
            coordinatesList = createCoordinatesList(waypointCount = 3)
        ),
        requestUuid = requestUuid
    )

private fun NavigationRoute.assertCongestionExpiredForLeg(legIndex: Int) {
    val legToCheck = this.directionsRoute.legs()!![legIndex].annotation()!!
    assertTrue(
        "Expected unknown congestion after expiration " +
            "but they are ${legToCheck.congestion()}",
        legToCheck.congestion()?.all { it == "unknown" } ?: false
    )
    assertTrue(
        "Expected null congestion numeric after expiration " +
            "but they are ${legToCheck.congestionNumeric()}",
        legToCheck.congestionNumeric()?.all { it == null } ?: false
    )
}

private fun DirectionsSession.onRefresh(
    body: (
        refreshAttempt: Int,
        route: NavigationRoute,
        currentIndices: CurrentIndices,
        callback: NavigationRouterRefreshCallback
    ) -> Unit
): DirectionsSession {
    var refreshAttempt = 0
    every { this@onRefresh.requestRouteRefresh(any(), any(), any()) } answers {
        body(refreshAttempt, firstArg(), secondArg(), thirdArg())
        refreshAttempt++
        refreshAttempt.toLong()
    }
    return this
}

private fun expectedTimeToInvalidateCongestions(refreshInterval: Long): Long = refreshInterval * 3

// in case of timeout controller will wait for the one more response
private fun expectedTimeToInvalidateCongestionsInCaseOfTimeout(refreshInterval: Long) =
    expectedTimeToInvalidateCongestions(refreshInterval) +
        refreshInterval

private fun createTestInitialAndRefreshedTestRoutes(): Pair<NavigationRoute, NavigationRoute> =
    Pair(
        createNavigationRoute(
            createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("moderate", "heavy"),
                    congestionNumeric = listOf(50, 94),
                ),
            )
        ),
        createNavigationRoute(
            createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("heavy", "heavy"),
                    congestionNumeric = listOf(93, 94),
                )
            )
        )
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> Deferred<T>.getCompletedTest(): T = if (isActive) {
    cancel()
    error("can't get result from a Deferred, coroutine is still active")
} else getCompleted()
