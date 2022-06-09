package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.testing.add
import com.mapbox.navigation.testing.factories.createCoordinatesList
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.utcToLocalTime
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
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
        val directionsSession = mockk<DirectionsSession>().apply {
            onRefresh { _, _, _, callback -> callback.onRefreshReady(refreshedRoute) }
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = directionsSession,
            routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build(),
        )

        val refreshJob = async { routeRefreshController.refresh(listOf(initialRoute)) }
        advanceTimeBy(TimeUnit.SECONDS.toMillis(30))

        assertEquals(listOf(refreshedRoute), refreshJob.getCompletedTest())
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `should refresh route with any annotation`() = runBlockingTest {
        val routeWithoutAnnotations = createTestTwoLegRoute(
            firstLegAnnotations = null,
            secondLegAnnotations = null
        )
        val refreshedRoute = createNavigationRoute()
        val directionsSession = mockk<DirectionsSession>().apply {
            onRefresh { _, _, _, callback -> callback.onRefreshReady(refreshedRoute) }
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = directionsSession
        )

        val refreshedRouteDeferred =
            async { routeRefreshController.refresh(listOf(routeWithoutAnnotations)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertEquals(listOf(refreshedRoute), refreshedRouteDeferred.getCompletedTest())
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should log warning when route is not supported`() = runBlockingTest {
        val primaryRoute = createTestTwoLegRoute(requestUuid = null)
        val directionsSession = mockk<DirectionsSession>()
        val routeRefreshController = createRouteRefreshController()

        val refreshedDeferred = async { routeRefreshController.refresh(listOf(primaryRoute)) }
        advanceTimeBy(TimeUnit.HOURS.toMillis(6))

        assertTrue(refreshedDeferred.isActive)
        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
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
        val directionsSession = mockk<DirectionsSession> {
            every { requestRouteRefresh(any(), any(), any()) } returns 8
            every { cancelRouteRefreshRequest(any()) } returns Unit
        }
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = directionsSession
        )

        val refreshJob = async { routeRefreshController.refresh(listOf(createTestTwoLegRoute())) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        refreshJob.cancel()

        verify(exactly = 1) { directionsSession.cancelRouteRefreshRequest(8) }
    }

    @Test
    fun `do not send a request when uuid is empty`() = runBlockingTest {
        val directionsSession = mockk<DirectionsSession>(relaxed = true)
        val routeRefreshController = createRouteRefreshController(
            routeRefresh = directionsSession
        )
        val route = createTestTwoLegRoute(requestUuid = "")

        val refreshDeferred = launch { routeRefreshController.refresh(listOf(route)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertTrue(refreshDeferred.isActive)
        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        refreshDeferred.cancel()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `refresh route without legs`() = runBlockingTest {
        val initialRoute = createNavigationRoute(
            createDirectionsRoute(
                legs = null,
                createRouteOptions(enableRefresh = true)
            )
        )
        val routeRefreshController = createRouteRefreshController()

        routeRefreshController.refresh(listOf(initialRoute))
    }

    @Test
    fun `should log route diffs when there is a successful response`() =
        runBlockingTest {
            val initialRoute = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("moderate", "heavy"),
                    congestionNumeric = listOf(50, 94)
                ),
            )
            val refreshedRoute = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("heavy", "heavy"),
                    congestionNumeric = listOf(93, 94),
                ),
                firstLegIncidents = listOf(
                    createIncident(id = "1")
                )
            )
            val directionsSession = mockk<DirectionsSession>().apply {
                onRefresh { _, _, _, callback -> callback.onRefreshReady(refreshedRoute) }
            }
            val routeRefreshController = createRouteRefreshController(
                routeRefresh = directionsSession,
            )

            val refreshJob = launch { routeRefreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

            assertTrue(refreshJob.isCompleted)
            verify {
                logger.logI(
                    "Updated congestion, congestionNumeric, incidents at leg 0",
                    RouteRefreshController.LOG_CATEGORY
                )
            }
        }

    @Test
    fun `should log message when there is a successful response without route diffs`() =
        runBlockingTest {
            val initialRoute = createTestTwoLegRoute()
            val refreshedRoute = createTestTwoLegRoute()
            val directionsSession = mockk<DirectionsSession>().onRefresh { _, _, _, callback ->
                callback.onRefreshReady(refreshedRoute)
            }

            val routeRefreshController = createRouteRefreshController(
                routeRefresh = directionsSession,
            )

            val refreshJob = launch { routeRefreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

            verify {
                logger.logI("No changes to route annotations", RouteRefreshController.LOG_CATEGORY)
            }
            refreshJob.cancel()
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
            val primaryRoute = createTestTwoLegRoute(
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
            val directionsSession = mockk<DirectionsSession>()
                .onRefresh { _, _, _, callback ->
                    callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
                }

            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                currentLegIndexProvider = { 0 },
                localDateProvider = { currentTime },
                routeRefresh = directionsSession,
            )
            // act
            val refreshedRoutesDeffer = async {
                routeRefreshController.refresh(listOf(primaryRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            // assert
            val refreshedRoute = refreshedRoutesDeffer.getCompletedTest().first()
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
            val initialRoute = createTestTwoLegRoute()
            val directionsSession = mockk<DirectionsSession>().onRefresh { _, _, _, callback ->
                callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeRefresh = directionsSession
            )
            val invalidatedRouteDeffer = async {
                routeRefreshController.refresh(listOf(initialRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            val invalidatedRoute = invalidatedRouteDeffer.getCompletedTest().first()
            // act
            val refreshedRoute = async {
                routeRefreshController.refresh(listOf(invalidatedRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis) * 100
            )
            assertFalse(refreshedRoute.isCompleted)
            directionsSession.onRefresh { _, _, _, callback ->
                callback.onRefreshReady(initialRoute)
            }
            advanceTimeBy(routeRefreshOptions.intervalMillis)
            // assert
            assertEquals(listOf(initialRoute), refreshedRoute.getCompletedTest())
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
            val initialRoute = createTestTwoLegRoute(
                firstLegIncidents = listOf(
                    createIncident(
                        id = "1",
                        endTime = "2022-05-22T12:00:00Z" // expires in 3 hours
                    )
                )
            )
            val directionsSession = mockk<DirectionsSession>().onRefresh { _, _, _, callback ->
                callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeRefresh = directionsSession,
                localDateProvider = { currentTime }
            )
            val invalidatedRouteDeffer = async {
                routeRefreshController.refresh(listOf(initialRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            val invalidatedRoute = invalidatedRouteDeffer.getCompletedTest().first()
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
                refreshedRoute.getCompletedTest().first()
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
            val currentRoute = createTestTwoLegRoute(
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
            val currentLegIndexProvider = { 1 }
            val directionsSession = mockk<DirectionsSession>(relaxed = true)
                .onRefresh { _, _, _, _ -> }
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000L)
                .build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeDiffProvider = DirectionsRouteDiffProvider(),
                currentLegIndexProvider = currentLegIndexProvider,
                localDateProvider = { currentTime },
                routeRefresh = directionsSession
            )
            // act
            val refreshedRoutesDeferred = async {
                routeRefreshController.refresh(listOf(currentRoute))
            }
            // in case of timeout controller will wait for the third response, so congestion will be
            // invalidated after 4X refresh intervals
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis) +
                    routeRefreshOptions.intervalMillis
            )
            // assert
            val refreshedRoute = refreshedRoutesDeferred.getCompletedTest().first()
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
            val initialRoute = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("severe", "moderate"),
                    congestionNumeric = listOf(90, 50),
                )
            )
            val refreshed = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("severe", "severe"),
                    congestionNumeric = listOf(90, 90),
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

            assertEquals(listOf(refreshed), refreshedDeferred.getCompletedTest())
        }

    @Test
    fun `route successfully refreshes on time if first try failed`() =
        runBlockingTest {
            val initialRoute = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("severe", "moderate"),
                    congestionNumeric = listOf(90, 50),
                )
            )
            val refreshed = createTestTwoLegRoute(
                firstLegAnnotations = createRouteLegAnnotation(
                    congestion = listOf("severe", "severe"),
                    congestionNumeric = listOf(90, 90),
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

            assertEquals(listOf(refreshed), refreshedDeferred.getCompletedTest())
        }

    private fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions = RouteRefreshOptions.Builder().build(),
        routeRefresh: RouteRefresh = mockk(),
        currentLegIndexProvider: () -> Int = { 0 },
        routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
        localDateProvider: () -> Date = { Date(1653493148247) }
    ) = RouteRefreshController(
        routeRefreshOptions,
        routeRefresh,
        currentLegIndexProvider,
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
) = createNavigationRoute(
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
        legIndex: Int,
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

private fun createTestInitialAndRefreshedTestRoutes(): Pair<NavigationRoute, NavigationRoute> =
    Pair(
        createTestTwoLegRoute(
            firstLegAnnotations = createRouteLegAnnotation(
                congestion = listOf("moderate", "heavy"),
                congestionNumeric = listOf(50, 94),
            ),
        ),
        createTestTwoLegRoute(
            firstLegAnnotations = createRouteLegAnnotation(
                congestion = listOf("heavy", "heavy"),
                congestionNumeric = listOf(93, 94),
            )
        )
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> Deferred<T>.getCompletedTest(): T = if (isActive) {
    cancel()
    error("can't get result from a Deferred, coroutine is still active")
} else getCompleted()
