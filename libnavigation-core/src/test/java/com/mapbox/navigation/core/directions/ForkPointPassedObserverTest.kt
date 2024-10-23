package com.mapbox.navigation.core.directions

import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.IgnoredRoute
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ForkPointPassedObserverTest {
    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private lateinit var directionsSession: DirectionsSession
    private lateinit var currentLegIndex: () -> Int
    private lateinit var observer: ForkPointPassedObserver

    @Before
    fun setUp() {
        directionsSession = mockk(relaxed = true)
        currentLegIndex = mockk()
        observer = ForkPointPassedObserver(directionsSession, currentLegIndex)
    }

    @Test
    fun `onRouteProgressChanged hides alternatives when fork point passed`() {
        val routeProgress: RouteProgress = mockk {
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route1" to mockk { every { isForkPointPassed } returns true },
                "route2" to mockk { every { isForkPointPassed } returns false },
            )
        }

        val route1 = mockk<NavigationRoute> { every { id } returns "route1" }
        val route2 = mockk<NavigationRoute> { every { id } returns "route2" }
        val routes = listOf(route1, route2)

        every { directionsSession.routes } returns routes
        every { currentLegIndex.invoke() } returns 0

        observer.onRouteProgressChanged(routeProgress)

        verify {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    listOf(route2),
                    listOf(
                        IgnoredRoute(
                            route1,
                            ForkPointPassedObserver.REASON_ALTERNATIVE_FORK_POINT_PASSED,
                        ),
                    ),
                    SetRoutes.Alternatives(0),
                ),
            )
        }
    }

    @Test
    fun `onRouteProgressChanged does nothing when no fork point passed`() {
        val mainRoute = mockk<NavigationRoute> { every { id } returns "route1" }
        val alternativeRoute = mockk<NavigationRoute> { every { id } returns "route2" }

        val routeProgress: RouteProgress = mockk {
            every { navigationRoute } returns mainRoute
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route2" to mockk { every { isForkPointPassed } returns false },
            )
        }

        val routes = listOf<NavigationRoute>(
            mainRoute,
            alternativeRoute,
        )

        every { directionsSession.routes } returns routes
        every { currentLegIndex.invoke() } returns 0

        observer.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { directionsSession.setNavigationRoutesFinished(any()) }
    }

    @Test
    fun `onRouteProgressChanged does nothing when no routes`() {
        val routeProgress: RouteProgress = mockk {
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route1" to mockk { every { isForkPointPassed } returns true },
            )
        }

        every { currentLegIndex.invoke() } returns 0
        every { directionsSession.routes } returns emptyList()

        observer.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) { directionsSession.setNavigationRoutesFinished(any()) }
    }

    @Test
    fun `ignored alternative becomes available and is returned to routes list`() {
        val route1 = mockk<NavigationRoute> { every { id } returns "route1" }
        val route2 = mockk<NavigationRoute> { every { id } returns "route2" }
        val currentRotues = listOf(route1, route2)

        val ignoredRoute = IgnoredRoute(
            navigationRoute = mockk<NavigationRoute> { every { id } returns "ignored_route" },
            reason = ForkPointPassedObserver.REASON_ALTERNATIVE_FORK_POINT_PASSED,
        )

        every { directionsSession.routes } returns currentRotues
        every { directionsSession.ignoredRoutes } returns listOf(ignoredRoute)
        every { currentLegIndex.invoke() } returns 0

        val routeProgress: RouteProgress = mockk {
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route1" to mockk(relaxed = true),
                "route2" to mockk(relaxed = true),
                "ignored_route" to mockk(relaxed = true),
            )
        }

        observer.onRouteProgressChanged(routeProgress)

        verify {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    currentRotues + ignoredRoute.navigationRoute,
                    emptyList(),
                    SetRoutes.Alternatives(0),
                ),
            )
        }
    }

    @Test
    fun `the same available and ignored routes should not trigger direction session update`() {
        val route1 = mockk<NavigationRoute> { every { id } returns "route1" }
        val route2 = mockk<NavigationRoute> { every { id } returns "route2" }
        val currentRotues = listOf(route1, route2)

        val ignoredRoute = IgnoredRoute(
            navigationRoute = mockk<NavigationRoute> { every { id } returns "ignored_route" },
            reason = ForkPointPassedObserver.REASON_ALTERNATIVE_FORK_POINT_PASSED,
        )

        every { directionsSession.routes } returns currentRotues
        every { directionsSession.ignoredRoutes } returns listOf(ignoredRoute)
        every { currentLegIndex.invoke() } returns 0

        val routeProgress: RouteProgress = mockk {
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route1" to mockk(relaxed = true),
                "route2" to mockk(relaxed = true),
                "ignored_route" to mockk(relaxed = true) {
                    every { isForkPointPassed } returns true
                },
            )
        }

        observer.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) {
            directionsSession.setNavigationRoutesFinished(any())
        }
    }

    @Test
    fun `alternative route with fork point passed from true to false returns to main routes`() {
        val route1 = mockk<NavigationRoute> { every { id } returns "route1" }
        val route2 = mockk<NavigationRoute> { every { id } returns "route2" }
        val ignoredRoute = IgnoredRoute(
            navigationRoute = mockk<NavigationRoute> { every { id } returns "ignored_route" },
            reason = ForkPointPassedObserver.REASON_ALTERNATIVE_FORK_POINT_PASSED,
        )

        every { directionsSession.routes } returns listOf(route1, route2)
        every { directionsSession.ignoredRoutes } returns listOf(ignoredRoute)
        every { currentLegIndex.invoke() } returns 0

        val routeProgress: RouteProgress = mockk {
            every { internalAlternativeRouteIndices() } returns mapOf(
                "route1" to mockk { every { isForkPointPassed } returns false },
                "route2" to mockk { every { isForkPointPassed } returns false },
                "ignored_route" to mockk { every { isForkPointPassed } returns false },
            )
        }

        observer.onRouteProgressChanged(routeProgress)

        verify {
            directionsSession.setNavigationRoutesFinished(
                DirectionsSessionRoutes(
                    listOf(route1, route2, ignoredRoute.navigationRoute),
                    emptyList(),
                    SetRoutes.Alternatives(0),
                ),
            )
        }
    }
}
