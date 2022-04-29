package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFactory
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.infra.factories.createCoordinatesList
import com.mapbox.navigation.core.infra.factories.createDirectionsRoute
import com.mapbox.navigation.core.infra.factories.createIncident
import com.mapbox.navigation.core.infra.factories.createNavigationRoute
import com.mapbox.navigation.core.infra.factories.createRouteLeg
import com.mapbox.navigation.core.infra.factories.createRouteLegAnnotation
import com.mapbox.navigation.core.infra.factories.createRouteOptions
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.Month
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class RouteRefreshControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @Before
    fun setup() {
        LoggerProvider.setLoggerFrontend(logger)
    }

    @Test
    fun `route with disabled refresh never refreshes`() = coroutineRule.runBlockingTest {
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
    fun `route refreshes`() = coroutineRule.runBlockingTest {
        val (initialRoute, refreshedRoute) = createTestInitialAndRefreshedTestRoutes()
        val directionsSession = mockk<DirectionsSession>().apply {
            onRefresh { _, _, callback -> callback.onRefreshReady(refreshedRoute) }
        }
        val routeRefreshController = createRouteRefreshController(
            directionsSession = directionsSession,
            routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build(),
        )

        val refreshJob = async { routeRefreshController.refresh(listOf(initialRoute)) }
        advanceTimeBy(TimeUnit.SECONDS.toMillis(30))

        assertEquals(listOf(refreshedRoute), refreshJob.getCompleted())
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 0) { directionsSession.cancelRouteRefreshRequest(any()) }
    }

    @Test
    fun `should refresh route with any annotation`() = coroutineRule.runBlockingTest {
        val routeWithoutAnnotations = createTestTwoLegRoute(
            firstLegAnnotations = null,
            secondLegAnnotations = null
        )
        val refreshedRoute = createNavigationRoute()
        val directionsSession = mockk<DirectionsSession>().apply {
            onRefresh { _, _, callback -> callback.onRefreshReady(refreshedRoute) }
        }
        val routeRefreshController = createRouteRefreshController(
            directionsSession = directionsSession
        )

        val refreshedRouteDeferred =
            async { routeRefreshController.refresh(listOf(routeWithoutAnnotations)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertEquals(listOf(refreshedRoute), refreshedRouteDeferred.getCompleted())
        verify(exactly = 1) { directionsSession.requestRouteRefresh(any(), any(), any()) }
    }

    @Test
    fun `should log warning when route is not supported`() = coroutineRule.runBlockingTest {
        val primaryRoute = createTestTwoLegRoute(requestUuid = null)
        val directionsSession = mockk<DirectionsSession>()
        val routeRefreshController = createRouteRefreshController()

        val refreshedDeferred = async { routeRefreshController.refresh(listOf(primaryRoute)) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertTrue(refreshedDeferred.isActive)
        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        verify(exactly = 1) {
            logger.logW(any(), any())
        }
        refreshedDeferred.cancel()
    }

    @Test
    fun `cancel request when stopped`() = coroutineRule.runBlockingTest {
        val directionsSession = mockk<DirectionsSession> {
            every { requestRouteRefresh(any(), any(), any()) } returns 8
            every { cancelRouteRefreshRequest(any()) } returns Unit
        }
        val routeRefreshController = createRouteRefreshController(
            directionsSession = directionsSession
        )

        val refreshJob = async { routeRefreshController.refresh(listOf(createTestTwoLegRoute())) }
        advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        refreshJob.cancel()

        verify(exactly = 1) { directionsSession.cancelRouteRefreshRequest(8) }
    }

    @Test
    fun `do not send a request when uuid is empty`() = coroutineRule.runBlockingTest {
        val directionsSession = mockk<DirectionsSession>(relaxed = true)
        val routeRefreshController = createRouteRefreshController(
            directionsSession = directionsSession
        )
        val route = createTestTwoLegRoute(requestUuid = "")

        val refreshDeferred = launch { routeRefreshController.refresh(listOf(route)) }
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

        assertTrue(refreshDeferred.isActive)
        verify(exactly = 0) { directionsSession.requestRouteRefresh(any(), any(), any()) }
        refreshDeferred.cancel()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `refresh route without legs`() = coroutineRule.runBlockingTest {
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
        coroutineRule.runBlockingTest {
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
            )
            val directionsSession = mockk<DirectionsSession>().apply {
                onRefresh { _, _, callback -> callback.onRefreshReady(refreshedRoute) }
            }
            val routeRefreshController = createRouteRefreshController(
                directionsSession = directionsSession,
            )

            val refreshJob = launch { routeRefreshController.refresh(listOf(initialRoute)) }
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

            assertTrue(refreshJob.isCompleted)
            verify {
                logger.logI(
                    "Updated congestion, congestionNumeric at leg 0",
                    RouteRefreshController.LOG_CATEGORY
                )
            }
        }

    @Test
    fun `should log message when there is a successful response without route diffs`() =
        coroutineRule.runBlockingTest {
            val initialRoute = createTestTwoLegRoute()
            val refreshedRoute = createTestTwoLegRoute()
            val directionsSession = mockk<DirectionsSession>().onRefresh { _, _, callback ->
                callback.onRefreshReady(refreshedRoute)
            }

            val routeRefreshController = createRouteRefreshController(
                directionsSession = directionsSession,
            )

            val refreshJob = launch { routeRefreshController.refresh(listOf(initialRoute)) }
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))

            verify {
                logger.logI("No changes to route annotations", RouteRefreshController.LOG_CATEGORY)
            }
            refreshJob.cancel()
        }

    @Test
    fun `traffic annotations and incidents on current leg(first) disappears if refresh fails for a long time`() =
        coroutineRule.runBlockingTest {
            val currentTime = LocalDateTime.of(
                2022,
                Month.MAY,
                22,
                13,
                0,
                0
            )
            val primaryRoute = createTestTwoLegRoute(
                firstLegIncidents = listOf(
                    createIncident(
                        id = "1",
                        endTime = "2022-05-22T14:00:00Z",
                    ),
                    createIncident(
                        id = "2",
                        endTime = "2022-05-22T12:00:00Z",
                    ),
                    createIncident(
                        id = "3",
                        endTime = null,
                    )
                ),
                secondLegIncidents = listOf(
                    createIncident(id = "4"),
                    createIncident(id = "5"),
                )
            )
            val directionsSession = mockk<DirectionsSession>() {
                var refreshAttempt = 0L
                every { requestRouteRefresh(any(), any(), any()) } answers {
                    val callback = thirdArg<NavigationRouterRefreshCallback>()
                    callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
                    refreshAttempt++
                    refreshAttempt
                }
            }

            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000)
                .build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                currentLegIndexProvider = { 0 },
                currentDateTimeProvider = { currentTime },
                directionsSession = directionsSession
            )
            // act
            val refreshedRoutesDeffer = async {
                routeRefreshController.refresh(listOf(primaryRoute))
            }
            coroutineRule.testDispatcher.advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            // assert
            val refreshedRoute = refreshedRoutesDeffer.getCompleted().first()
            refreshedRoute.assertCongestionExpiredForLeg(0)
            assertEquals(
                listOf("1"),
                refreshedRoute.directionsRoute.legs()!![0].incidents()?.map { it.id() }
            )
            assertEquals(
                "ony current(first) leg should be refreshed",
                primaryRoute.directionsRoute.legs()!![1].annotation(),
                refreshedRoute.directionsRoute.legs()!![1].annotation()
            )
            assertEquals(
                "ony current(first) leg should be refreshed",
                primaryRoute.directionsRoute.legs()!![1].incidents(),
                refreshedRoute.directionsRoute.legs()!![1].incidents()
            )
        }

    @Test
    fun `after invalidation route isn't updated until successful refresh`() =
        coroutineRule.runBlockingTest {
            val initialRoute = createTestTwoLegRoute()
            val directionsSession = mockk<DirectionsSession>().onRefresh { _, _, callback ->
                callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder().build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                directionsSession = directionsSession
            )
            val invalidatedRouteDeffer = async {
                routeRefreshController.refresh(listOf(initialRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis)
            )
            val invalidatedRoute = invalidatedRouteDeffer.getCompleted().first()
            // act
            val refreshedRoute = async {
                routeRefreshController.refresh(listOf(invalidatedRoute))
            }
            advanceTimeBy(
                expectedTimeToInvalidateCongestions(routeRefreshOptions.intervalMillis) * 100
            )
            assertFalse(refreshedRoute.isCompleted)
            directionsSession.onRefresh { _, _, callback ->
                callback.onRefreshReady(initialRoute)
            }
            advanceTimeBy(routeRefreshOptions.intervalMillis)
            // assert
            assertEquals(listOf(initialRoute), refreshedRoute.getCompleted())
        }

    @Test
    fun `traffic annotations and expired annotations on current leg(second) disappear if refresh doesn't respond`() =
        coroutineRule.runBlockingTest {
            val currentTime = LocalDateTime.of(
                2022,
                Month.MAY,
                22,
                13,
                0,
                0
            )
            val currentRoute = createTestTwoLegRoute(
                firstLegIncidents = listOf(
                    createIncident(
                        id = "1",
                        endTime = "2022-05-22T10:00:00Z",
                    ),
                ),
                secondLegIncidents = listOf(
                    createIncident(
                        id = "2",
                        endTime = "2022-05-22T10:00:00Z"
                    ),
                    createIncident(
                        id = "3",
                        endTime = "2022-05-22T13:00:01Z"
                    ),
                )
            )
            val currentLegIndexProvider = { 1 }
            val directionsSession = mockk<DirectionsSession>(relaxed = true) {
                var refreshAttempt = 0L
                every { requestRouteRefresh(any(), any(), any()) } answers {
                    refreshAttempt++
                    refreshAttempt
                }
            }
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(30_000L)
                .build()
            val routeRefreshController = createRouteRefreshController(
                routeRefreshOptions = routeRefreshOptions,
                routeDiffProvider = DirectionsRouteDiffProvider(),
                currentLegIndexProvider = currentLegIndexProvider,
                currentDateTimeProvider = { currentTime },
                directionsSession = directionsSession
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
            val refreshedRoute = refreshedRoutesDeferred.getCompleted().first()
            refreshedRoute.assertCongestionExpiredForLeg(1)
            assertEquals(
                listOf("3"),
                refreshedRoute.directionsRoute.legs()!![1].incidents()?.map { it.id() }
            )
            assertEquals(
                "ony current(second) leg should be refreshed",
                currentRoute.directionsRoute.legs()!![0].annotation(),
                refreshedRoute.directionsRoute.legs()!![0].annotation()!!
            )
            assertEquals(
                "ony current(second) leg should be refreshed",
                currentRoute.directionsRoute.legs()!![0].incidents(),
                refreshedRoute.directionsRoute.legs()!![0].incidents()!!
            )
        }

    @Test
    fun `route successfully refreshes on time if first try doesn't respond`() =
        coroutineRule.runBlockingTest {
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
            val directionsSession = mockk<DirectionsSession>(relaxed = true) {
                var refreshAttempt = 0L
                every { requestRouteRefresh(any(), any(), any()) } answers {
                    if (refreshAttempt >= 1) {
                        val callback = thirdArg<NavigationRouterRefreshCallback>()
                        callback.onRefreshReady(currentRoute)
                    }
                    refreshAttempt++
                }
            }
            val refreshInterval = 30_000L
            val refreshController = createRouteRefreshController(
                directionsSession = directionsSession,
                routeRefreshOptions = RouteRefreshOptions.Builder()
                    .intervalMillis(refreshInterval)
                    .build()
            )

            val refreshedDeferred = async { refreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(refreshInterval)
            assertFalse(refreshedDeferred.isCompleted)
            currentRoute = refreshed
            advanceTimeBy(refreshInterval)

            assertEquals(listOf(refreshed), refreshedDeferred.getCompleted())
        }

    @Test
    fun `route successfully refreshes on time if first try failed`() =
        coroutineRule.runBlockingTest {
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
            val directionsSession = mockk<DirectionsSession>(relaxed = true) {
                var refreshAttempt = 0L
                every { requestRouteRefresh(any(), any(), any()) } answers {
                    val callback = thirdArg<NavigationRouterRefreshCallback>()
                    if (refreshAttempt >= 1) {
                        callback.onRefreshReady(currentRoute)
                    } else {
                        callback.onFailure(RouterFactory.buildNavigationRouterRefreshError())
                    }
                    refreshAttempt++
                }
            }
            val refreshInterval = 30_000L
            val refreshController = createRouteRefreshController(
                directionsSession = directionsSession,
                routeRefreshOptions = RouteRefreshOptions.Builder()
                    .intervalMillis(refreshInterval)
                    .build()
            )

            val refreshedDeferred = async { refreshController.refresh(listOf(initialRoute)) }
            advanceTimeBy(refreshInterval)
            assertFalse(refreshedDeferred.isCompleted)
            currentRoute = refreshed
            advanceTimeBy(refreshInterval)

            assertEquals(listOf(refreshed), refreshedDeferred.getCompleted())
        }

    private fun createRouteRefreshController(
        routeRefreshOptions: RouteRefreshOptions = RouteRefreshOptions.Builder().build(),
        directionsSession: DirectionsSession = mockk(),
        currentLegIndexProvider: () -> Int = { 0 },
        routeDiffProvider: DirectionsRouteDiffProvider = DirectionsRouteDiffProvider(),
        currentDateTimeProvider: () -> LocalDateTime = LocalDateTime::now
    ) = RouteRefreshController(
        routeRefreshOptions,
        directionsSession,
        currentLegIndexProvider,
        routeDiffProvider,
        currentDateTimeProvider
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
        route: NavigationRoute,
        legIndex: Int,
        callback: NavigationRouterRefreshCallback
    ) -> Unit
): DirectionsSession {
    var refreshAttempt = 0L
    every { this@onRefresh.requestRouteRefresh(any(), any(), any()) } answers {
        body(firstArg(), secondArg(), thirdArg())
        refreshAttempt++
        refreshAttempt
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
