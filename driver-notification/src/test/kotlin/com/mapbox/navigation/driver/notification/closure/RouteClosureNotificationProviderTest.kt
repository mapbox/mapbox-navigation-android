package com.mapbox.navigation.driver.notification.closure

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.isClosureAlternative
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.mapbox.api.directions.v5.models.Incident as DirectionsIncident

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class RouteClosureNotificationProviderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(MapboxNavigation::flowRouteProgress)
        mockkStatic(MapboxNavigation::flowRoutesUpdated)
        mockkStatic("com.mapbox.navigation.base.internal.route.NavigationRouteEx")
    }

    @After
    fun tearDown() {
        unmockkStatic(MapboxNavigation::flowRouteProgress)
        unmockkStatic(MapboxNavigation::flowRoutesUpdated)
        unmockkStatic("com.mapbox.navigation.base.internal.route.NavigationRouteEx")
    }

    @Test
    fun `emits monitoring notification for far closure`() = coroutineRule.runBlockingTest {
        every { any<MapboxNavigation>().flowRouteProgress() } answers {
            flowOf(routeProgressWithClosure(INCIDENT_ID, FAR_DISTANCE_M))
        }
        every { any<MapboxNavigation>().flowRoutesUpdated() } answers { emptyFlow() }

        val result = provider().trackNotifications()
            .firstOrNull { it is RouteClosureMonitoringNotification }
            as? RouteClosureMonitoringNotification

        assertNotNull(result)
        assertEquals(INCIDENT_ID, result?.incidentId)
        assertEquals(FAR_DISTANCE_M, result?.distanceMeters)
    }

    @Test
    fun `emits alternative when closure is exactly at vehicle position (distanceToStart == 0)`() =
        coroutineRule.runBlockingTest {
            val altRoute = cleanRoute("alt-clean")
            val progress = routeProgressWithClosure(INCIDENT_ID, 0.0)
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                flowOf(closureUpdate(listOf(primaryRoute(), altRoute)))
            }

            val result = provider().trackNotifications()
                .firstOrNull { it is RouteClosureAlternativeNotification }
                as? RouteClosureAlternativeNotification

            assertNotNull(result)
            assertEquals(INCIDENT_ID, result?.incidentId)
        }

    @Test
    fun `emits resolved when no closure in upcomingRoadObjects`() = coroutineRule.runBlockingTest {
        every { any<MapboxNavigation>().flowRouteProgress() } answers {
            flowOf(routeProgressWithoutClosure())
        }
        every { any<MapboxNavigation>().flowRoutesUpdated() } answers { emptyFlow() }

        val result =
            provider().trackNotifications().firstOrNull { it is RouteClosureResolvedNotification }

        assertNotNull(result)
    }

    @Test
    fun `emits resolved when closure is at or within threshold (close)`() =
        coroutineRule.runBlockingTest {
            every { any<MapboxNavigation>().flowRouteProgress() } answers {
                flowOf(routeProgressWithClosure(INCIDENT_ID, CLOSE_DISTANCE_M))
            }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers { emptyFlow() }

            val result = provider().trackNotifications()
                .firstOrNull { it is RouteClosureResolvedNotification }

            assertNotNull(result)
        }

    @Test
    fun `emits alternative notification for close closure and closure update`() =
        coroutineRule.runBlockingTest {
            val altRoute = cleanRoute("alt-clean")
            val progress = routeProgressWithClosure(INCIDENT_ID, CLOSE_DISTANCE_M)
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                flowOf(closureUpdate(listOf(primaryRoute(), altRoute)))
            }

            val result = provider().trackNotifications()
                .firstOrNull { it is RouteClosureAlternativeNotification }
                as? RouteClosureAlternativeNotification

            assertNotNull(result)
            assertEquals(INCIDENT_ID, result?.incidentId)
            assertEquals(CLOSE_DISTANCE_M, result?.distanceMeters)
            assertEquals(altRoute.id, result?.alternativeRoute?.id)
        }

    @Test
    fun `does not emit alternative when closure is far (beyond threshold)`() =
        coroutineRule.runBlockingTest {
            val progress = routeProgressWithClosure(INCIDENT_ID, FAR_DISTANCE_M)
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                flowOf(closureUpdate(listOf(primaryRoute(), cleanRoute())))
            }

            val result = withTimeoutOrNull(300) {
                provider().trackNotifications()
                    .firstOrNull { it is RouteClosureAlternativeNotification }
            }

            assertNull(result)
        }

    @Test
    fun `does not emit alternative when update has no closure alternative`() =
        coroutineRule.runBlockingTest {
            val progress = routeProgressWithClosure(INCIDENT_ID, CLOSE_DISTANCE_M)
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                // primary plus a regular (non-closure) alternative
                val routes = listOf(primaryRoute(), regularAlternative())
                flowOf(updateWithoutClosureAlternative(routes))
            }

            val result = withTimeoutOrNull(300) {
                provider().trackNotifications()
                    .firstOrNull { it is RouteClosureAlternativeNotification }
            }

            assertNull(result)
        }

    // Scenario: [a0, a1, a2] -> closure on a0 -> [a0, b1, b2] (notification) ->
    // switch to b1 -> [b1, a0, b2] -> pass fork to a0 -> [b1, b2].
    // The fork-pass update still carries closure routes, but the live primary (b1) routes around
    // the closure, so no closure remains upcoming. The notification must NOT fire again.
    @Test
    fun `does not re-emit alternative after switching onto a closure-avoiding primary`() =
        coroutineRule.runBlockingTest {
            // Live primary already avoids the closure: no upcoming closure incident.
            val progress = routeProgressWithoutClosure()
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            // Routes still carry reason=closure from when they were generated.
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                flowOf(closureUpdate(listOf(cleanRoute("b1"), cleanRoute("b2"))))
            }

            val result = withTimeoutOrNull(300) {
                provider().trackNotifications()
                    .firstOrNull { it is RouteClosureAlternativeNotification }
            }

            assertNull(result)
        }

    @Test
    fun `alternative carries the route at index 1 from the closure update`() =
        coroutineRule.runBlockingTest {
            val altRoute = cleanRoute("alt-1")
            val progress = routeProgressWithClosure(INCIDENT_ID, CLOSE_DISTANCE_M)
            every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(progress) }
            every { any<MapboxNavigation>().flowRoutesUpdated() } answers {
                flowOf(closureUpdate(listOf(primaryRoute(), altRoute, cleanRoute("alt-2"))))
            }

            val result = provider().trackNotifications()
                .firstOrNull { it is RouteClosureAlternativeNotification }
                as? RouteClosureAlternativeNotification

            assertNotNull(result)
            assertEquals(altRoute.id, result?.alternativeRoute?.id)
        }

    @Test
    fun `no notifications emitted after onDetached`() = coroutineRule.runBlockingTest {
        every { any<MapboxNavigation>().flowRouteProgress() } answers {
            flowOf(routeProgressWithClosure(INCIDENT_ID, FAR_DISTANCE_M))
        }
        every { any<MapboxNavigation>().flowRoutesUpdated() } answers { emptyFlow() }

        val p = provider()
        p.onDetached(mapboxNavigation)

        val result = withTimeoutOrNull(100) {
            p.trackNotifications().firstOrNull()
        }

        assertNull(result)
    }

    private fun provider(
        thresholdMeters: Double = DEFAULT_THRESHOLD_M,
    ): RouteClosureNotificationProvider {
        val p = RouteClosureNotificationProvider(
            RouteClosureNotificationOptions.Builder()
                .alternativeTriggerThresholdMeters(thresholdMeters)
                .build(),
        )
        p.onAttached(mapboxNavigation)
        return p
    }

    private fun routeProgressWithClosure(
        incidentId: String,
        distanceMeters: Double,
    ): RouteProgress = mockk {
        every { upcomingRoadObjects } returns listOf(
            mockk<UpcomingRoadObject> {
                every { roadObject } returns mockk<Incident> {
                    every { id } returns incidentId
                    every { info } returns mockk<IncidentInfo>(relaxed = true) {
                        every { isClosed } returns true
                        every { type } returns IncidentType.ROAD_CLOSURE
                    }
                }
                every { distanceToStart } returns distanceMeters
            },
        )
    }

    private fun routeProgressWithoutClosure(): RouteProgress = mockk {
        every { upcomingRoadObjects } returns emptyList()
    }

    private fun routesUpdate(routes: List<NavigationRoute>): RoutesUpdatedResult =
        mockk(relaxed = true) {
            every { navigationRoutes } returns routes
        }

    /** Update carrying at least one closure alternative. */
    private fun closureUpdate(routes: List<NavigationRoute>): RoutesUpdatedResult =
        routesUpdate(routes)

    /** Update where no route is a closure alternative. */
    private fun updateWithoutClosureAlternative(
        routes: List<NavigationRoute>,
    ): RoutesUpdatedResult = routesUpdate(routes)

    private fun primaryRoute(): NavigationRoute = mockk(relaxed = true) {
        every { id } returns "primary"
        every { isClosureAlternative() } returns false
    }

    private fun regularAlternative(id: String = "alt-regular"): NavigationRoute =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { isClosureAlternative() } returns false
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(
                    mockk(relaxed = true) {
                        every { incidents() } returns emptyList()
                    },
                )
            }
        }

    private fun cleanRoute(id: String = "alt-clean"): NavigationRoute = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { isClosureAlternative() } returns true
        every { directionsRoute } returns mockk(relaxed = true) {
            every { legs() } returns listOf(
                mockk(relaxed = true) {
                    every { incidents() } returns emptyList()
                },
            )
        }
    }

    private fun routeWithClosure(id: String = "alt-with-closure"): NavigationRoute =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { isClosureAlternative() } returns true
            every { directionsRoute } returns mockk(relaxed = true) {
                every { legs() } returns listOf(
                    mockk(relaxed = true) {
                        every { incidents() } returns listOf(
                            mockk(relaxed = true) {
                                every { closed() } returns true
                                every { type() } returns DirectionsIncident.INCIDENT_ROAD_CLOSURE
                            },
                        )
                    },
                )
            }
        }

    private companion object {

        private const val DEFAULT_THRESHOLD_M = 250_000.0
        private const val FAR_DISTANCE_M = 300_000.0
        private const val CLOSE_DISTANCE_M = 200_000.0
        private const val INCIDENT_ID = "closure-1"
    }
}
