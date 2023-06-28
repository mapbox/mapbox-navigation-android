package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.internal.route.RouteExpirationHandler
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.core.RoutesRefreshData
import com.mapbox.navigation.core.RoutesRefreshDataProvider
import com.mapbox.navigation.core.directions.session.RouteRefresh
import com.mapbox.navigation.core.ev.EVRefreshDataProvider
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRefresherTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val route1Id = "id#1"
    private val route2Id = "id#2"
    private val route1 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns route1Id
    }
    private val route2 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns route2Id
    }
    private val newRoute1 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns route1Id
    }
    private val newRoute2 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns route2Id
    }
    private val legIndex1 = 1
    private val legIndex2 = 4
    private val legIndex3 = 7
    private val routesProgressData1 = RouteProgressData(legIndex1, 2, 3)
    private val routesProgressData2 = RouteProgressData(legIndex2, 5, 6)
    private val routesProgressData3 = RouteProgressData(legIndex3, 8, 9)
    private val routesRefreshData = RoutesRefreshData(
        route1,
        routesProgressData1,
        listOf(route2 to routesProgressData2)
    )

    private val routesRefreshDataProvider = mockk<RoutesRefreshDataProvider>(relaxed = true)
    private val evRefreshDataProvider = mockk<EVRefreshDataProvider>(relaxed = true)
    private val routeDiffProvider = mockk<DirectionsRouteDiffProvider>(relaxed = true)
    private val routeRefresh = mockk<RouteRefresh>(relaxed = true)
    private val sut = RouteRefresher(
        routesRefreshDataProvider,
        evRefreshDataProvider,
        routeDiffProvider,
        routeRefresh,
    )

    @Before
    fun setUp() {
        mockkObject(RouteExpirationHandler)
        mockkObject(RouteRefreshValidator)
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(listOf(route1, route2))
        } returns routesRefreshData
    }

    @After
    fun tearDown() {
        unmockkObject(RouteExpirationHandler)
        unmockkObject(RouteRefreshValidator)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refresh_emptyList() = coroutineRule.runBlockingTest {
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(emptyList())
        } throws IllegalArgumentException()

        sut.refresh(emptyList(), 0)
    }

    @Test
    fun refresh_allRoutesAreRefreshed() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute1)
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        every { routeDiffProvider.buildRouteDiffs(route1, newRoute1, legIndex1) } returns listOf(
            "diff#1",
            "diff#2"
        )
        every { routeDiffProvider.buildRouteDiffs(route2, newRoute2, legIndex2) } returns listOf(
            "diff#3",
            "diff#4"
        )
        val expected = RoutesRefresherResult(
            RouteRefresherResult(newRoute1, routesProgressData1, RouteRefresherStatus.SUCCESS),
            listOf(
                RouteRefresherResult(newRoute2, routesProgressData2, RouteRefresherStatus.SUCCESS)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)
        assertEquals(expected, actual)
        verify(exactly = 1) {
            logger.logI("Received refreshed route $route1Id", "RouteRefreshController")
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
            logger.logI("diff#1", "RouteRefreshController")
            logger.logI("diff#2", "RouteRefreshController")
            logger.logI("diff#3", "RouteRefreshController")
            logger.logI("diff#4", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_allRoutesAreRefreshed_noDiff() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute1)
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        every {
            routeDiffProvider.buildRouteDiffs(route1, newRoute1, legIndex1)
        } returns emptyList()
        every {
            routeDiffProvider.buildRouteDiffs(route2, newRoute2, legIndex2)
        } returns emptyList()

        sut.refresh(listOf(route1, route2), 10)

        verify(exactly = 1) {
            logger.logI("Received refreshed route $route1Id", "RouteRefreshController")
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
            logger.logI("No changes in annotations for route $route1Id", "RouteRefreshController")
            logger.logI("No changes in annotations for route $route2Id", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_onlyOneRouteIsRefreshed() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message"
                    every { throwable } returns ConcurrentModificationException()
                    every { refreshTtl } returns null
                }
            )
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        every { routeDiffProvider.buildRouteDiffs(any(), any(), any()) } returns listOf(
            "diff#1",
            "diff#2"
        )
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.FAILURE),
            listOf(
                RouteRefresherResult(newRoute2, routesProgressData2, RouteRefresherStatus.SUCCESS)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)

        assertEquals(expected, actual)
        verify(exactly = 1) {
            logger.logE(
                "Route refresh error: error message " +
                    "throwable=java.util.ConcurrentModificationException",
                "RouteRefreshController"
            )
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
            logger.logI("diff#1", "RouteRefreshController")
            logger.logI("diff#2", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_allRoutesRefreshFail() = coroutineRule.runBlockingTest {
        val route1 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "moderate"))
                            .congestionNumeric(listOf(80, 80))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T21:59:00Z"),
                            createIncident(endTime = "2022-06-31T21:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "heavy"))
                            .congestionNumeric(listOf(90, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T20:59:00Z"),
                            createIncident(endTime = "bad time"),
                            createIncident(endTime = "2022-06-30T19:59:00Z"),
                        )
                    ),
                )
            )
        )
        val route2 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "heavy"))
                            .congestionNumeric(listOf(80, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T10:59:00Z"),
                            createIncident(endTime = "2022-06-21T10:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "moderate"))
                            .congestionNumeric(listOf(90, 80))
                            .build(),
                        incidents = null
                    ),
                    createRouteLeg(
                        annotation = null,
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder().build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:50:00Z"),
                        )
                    ),
                )
            )
        )
        val route3 = createNavigationRoute(directionsRoute = createDirectionsRoute(legs = null))
        val routesRefreshData = RoutesRefreshData(
            route1,
            routesProgressData1,
            listOf(route2 to routesProgressData2, route3 to routesProgressData3)
        )
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(listOf(route1, route2, route3))
        } returns routesRefreshData
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 1"
                    every { throwable } returns ConcurrentModificationException()
                    every { refreshTtl } returns null
                }
            )
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 2"
                    every { throwable } returns IllegalStateException()
                    every { refreshTtl } returns null
                }
            )
            0
        }
        every { routeRefresh.requestRouteRefresh(route3, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 3"
                    every { throwable } returns IndexOutOfBoundsException()
                    every { refreshTtl } returns null
                }
            )
            0
        }
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.FAILURE),
            listOf(
                RouteRefresherResult(route2, routesProgressData2, RouteRefresherStatus.FAILURE),
                RouteRefresherResult(route3, routesProgressData3, RouteRefresherStatus.FAILURE)
            )
        )

        val actual = sut.refresh(listOf(route1, route2, route3), 10)

        assertEquals(expected, actual)
        verify(exactly = 1) {
            logger.logE(
                "Route refresh error: error message 1 " +
                    "throwable=java.util.ConcurrentModificationException",
                "RouteRefreshController"
            )
            logger.logE(
                "Route refresh error: error message 2 " +
                    "throwable=java.lang.IllegalStateException",
                "RouteRefreshController"
            )
            logger.logE(
                "Route refresh error: error message 3 " +
                    "throwable=java.lang.IndexOutOfBoundsException",
                "RouteRefreshController"
            )
        }
    }

    @Test
    fun refresh_oneRouteInvalidatedAfterRefreshOthersFail() = coroutineRule.runBlockingTest {
        val route1 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "moderate"))
                            .congestionNumeric(listOf(80, 80))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T21:59:00Z"),
                            createIncident(endTime = "2022-06-31T21:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "heavy"))
                            .congestionNumeric(listOf(90, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T20:59:00Z"),
                            createIncident(endTime = "bad time"),
                            createIncident(endTime = "2022-06-30T19:59:00Z"),
                        )
                    ),
                )
            )
        )
        val route2 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "heavy"))
                            .congestionNumeric(listOf(80, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T10:59:00Z"),
                            createIncident(endTime = "2022-06-21T10:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "moderate"))
                            .congestionNumeric(listOf(90, 80))
                            .build(),
                        incidents = null
                    ),
                    createRouteLeg(
                        annotation = null,
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:59:00Z"),
                        )
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder().build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:50:00Z"),
                        )
                    ),
                )
            )
        )
        val route3 = createNavigationRoute(directionsRoute = createDirectionsRoute(legs = null))
        val routesRefreshData = RoutesRefreshData(
            route1,
            routesProgressData1,
            listOf(route2 to routesProgressData2, route3 to routesProgressData3)
        )
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(listOf(route1, route2, route3))
        } returns routesRefreshData
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 1"
                    every { throwable } returns ConcurrentModificationException()
                    every { refreshTtl } returns 5
                }
            )
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 2"
                    every { throwable } returns IllegalStateException()
                    every { refreshTtl } returns 0
                }
            )
            0
        }
        every { routeRefresh.requestRouteRefresh(route3, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onFailure(
                mockk(relaxed = true) {
                    every { message } returns "error message 3"
                    every { throwable } returns IndexOutOfBoundsException()
                    every { refreshTtl } returns null
                }
            )
            0
        }
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.FAILURE),
            listOf(
                RouteRefresherResult(route2, routesProgressData2, RouteRefresherStatus.INVALIDATED),
                RouteRefresherResult(route3, routesProgressData3, RouteRefresherStatus.FAILURE)
            )
        )

        val actual = sut.refresh(listOf(route1, route2, route3), 10)

        assertEquals(expected, actual)
        verify(exactly = 1) {
            logger.logE(
                "Route refresh error: error message 1 " +
                    "throwable=java.util.ConcurrentModificationException",
                "RouteRefreshController"
            )
            logger.logE(
                "Route refresh error: error message 2 " +
                    "throwable=java.lang.IllegalStateException",
                "RouteRefreshController"
            )
            logger.logE(
                "Route refresh error: error message 3 " +
                    "throwable=java.lang.IndexOutOfBoundsException",
                "RouteRefreshController"
            )
        }
    }

    @Test
    fun refresh_oneRouteRefreshFailsByTimeout() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(any())
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } returns 0
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.FAILURE),
            listOf(
                RouteRefresherResult(newRoute2, routesProgressData2, RouteRefresherStatus.SUCCESS)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)

        assertEquals(expected, actual)
        verify(exactly = 1) {
            logger.logI(
                "Route refresh for route $route1Id was cancelled after timeout",
                "RouteRefreshController"
            )
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_oneRouteIsInvalid() = coroutineRule.runBlockingTest {
        val reason = "some reason"
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Invalid(reason)
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.INVALID),
            listOf(
                RouteRefresherResult(newRoute2, routesProgressData2, RouteRefresherStatus.SUCCESS)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)
        assertEquals(expected, actual)

        verify {
            logger.logI(
                "route $route1Id can't be refreshed because $reason",
                "RouteRefreshController"
            )
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_allRoutesAreInvalid() = coroutineRule.runBlockingTest {
        val route1Id = "route1"
        val reason1 = "some reason 1"
        val route2Id = "route2"
        val reason2 = "some reason 2"
        val route1 = spyk(
            createNavigationRoute(
                directionsRoute = createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("moderate", "moderate"))
                                .congestionNumeric(listOf(80, 80))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-30T21:59:00Z"),
                                createIncident(endTime = "2022-06-31T21:59:00Z"),
                            )
                        ),
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("heavy", "heavy"))
                                .congestionNumeric(listOf(90, 90))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-30T20:59:00Z"),
                                createIncident(endTime = "bad time"),
                                createIncident(endTime = "2022-06-30T19:59:00Z"),
                            )
                        ),
                    )
                )
            )
        ) {
            every { id } returns route1Id
        }
        val route2 = spyk(
            createNavigationRoute(
                directionsRoute = createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("moderate", "heavy"))
                                .congestionNumeric(listOf(80, 90))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-31T10:59:00Z"),
                                createIncident(endTime = "2022-06-21T10:59:00Z"),
                            )
                        ),
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("heavy", "moderate"))
                                .congestionNumeric(listOf(90, 80))
                                .build(),
                            incidents = null
                        ),
                    )
                )
            )
        ) { every { id } returns route2Id }
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Invalid(reason1)
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Invalid(reason2)
        val routesRefreshData = RoutesRefreshData(
            route1,
            routesProgressData1,
            listOf(route2 to routesProgressData2)
        )
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(listOf(route1, route2))
        } returns routesRefreshData
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.INVALID),
            listOf(RouteRefresherResult(route2, routesProgressData2, RouteRefresherStatus.INVALID))
        )

        val actual = sut.refresh(listOf(route1, route2), 10)
        assertEquals(expected, actual)

        verify {
            logger.logI(
                "route $route1Id can't be refreshed because $reason1",
                "RouteRefreshController"
            )
            logger.logI(
                "route $route2Id can't be refreshed because $reason2",
                "RouteRefreshController"
            )
        }
    }

    @Test
    fun refresh_oneRouteIsInvalidated() = coroutineRule.runBlockingTest {
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { RouteExpirationHandler.isRouteExpired(route1) } returns true
        every { RouteExpirationHandler.isRouteExpired(route2) } returns false
        every { routeRefresh.requestRouteRefresh(route1, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute1)
            0
        }
        every { routeRefresh.requestRouteRefresh(route2, any(), any()) } answers {
            (args[2] as NavigationRouterRefreshCallback).onRefreshReady(newRoute2)
            0
        }
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.INVALIDATED),
            listOf(
                RouteRefresherResult(newRoute2, routesProgressData2, RouteRefresherStatus.SUCCESS)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)
        assertEquals(expected, actual)

        verify {
            logger.logI(
                "route $route1Id will not be refreshed because it is invalidated",
                "RouteRefreshController"
            )
            logger.logI("Received refreshed route $route2Id", "RouteRefreshController")
        }
    }

    @Test
    fun refresh_allRoutesAreInvalidated() = coroutineRule.runBlockingTest {
        val route1Id = "route1"
        val route2Id = "route2"
        val route1 = spyk(
            createNavigationRoute(
                directionsRoute = createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("moderate", "moderate"))
                                .congestionNumeric(listOf(80, 80))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-30T21:59:00Z"),
                                createIncident(endTime = "2022-06-31T21:59:00Z"),
                            )
                        ),
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("heavy", "heavy"))
                                .congestionNumeric(listOf(90, 90))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-30T20:59:00Z"),
                                createIncident(endTime = "bad time"),
                                createIncident(endTime = "2022-06-30T19:59:00Z"),
                            )
                        ),
                    )
                )
            )
        ) {
            every { id } returns route1Id
        }
        val route2 = spyk(
            createNavigationRoute(
                directionsRoute = createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("moderate", "heavy"))
                                .congestionNumeric(listOf(80, 90))
                                .build(),
                            incidents = listOf(
                                createIncident(endTime = "2022-06-31T10:59:00Z"),
                                createIncident(endTime = "2022-06-21T10:59:00Z"),
                            )
                        ),
                        createRouteLeg(
                            annotation = LegAnnotation.builder()
                                .congestion(listOf("heavy", "moderate"))
                                .congestionNumeric(listOf(90, 80))
                                .build(),
                            incidents = null
                        ),
                    )
                )
            )
        ) { every { id } returns route2Id }
        every {
            RouteRefreshValidator.validateRoute(route1)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every {
            RouteRefreshValidator.validateRoute(route2)
        } returns RouteRefreshValidator.RouteValidationResult.Valid
        every { RouteExpirationHandler.isRouteExpired(any()) } returns true
        val routesRefreshData = RoutesRefreshData(
            route1,
            routesProgressData1,
            listOf(route2 to routesProgressData2)
        )
        coEvery {
            routesRefreshDataProvider.getRoutesRefreshData(listOf(route1, route2))
        } returns routesRefreshData
        val expected = RoutesRefresherResult(
            RouteRefresherResult(route1, routesProgressData1, RouteRefresherStatus.INVALIDATED),
            listOf(
                RouteRefresherResult(route2, routesProgressData2, RouteRefresherStatus.INVALIDATED)
            )
        )

        val actual = sut.refresh(listOf(route1, route2), 10)
        assertEquals(expected, actual)

        verify {
            logger.logI(
                "route $route1Id will not be refreshed because it is invalidated",
                "RouteRefreshController"
            )
            logger.logI(
                "route $route2Id will not be refreshed because it is invalidated",
                "RouteRefreshController"
            )
        }
    }
}
