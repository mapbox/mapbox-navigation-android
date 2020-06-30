package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsProvider
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class FasterRouteControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val directionsSession: DirectionsSession = mockk()
    private val tripSession: TripSession = mockk {
        every { getRouteProgress() } returns mockk()
    }
    private val fasterRouteObserver: FasterRouteObserver = mockk {
        every { restartAfterMillis() } returns FasterRouteObserver.DEFAULT_INTERVAL_MILLIS
        every { onFasterRoute(any(), any(), any()) } returns Unit
    }
    private val routesRequestCallbacks = slot<RoutesRequestCallback>()
    private val logger: Logger = mockk()
    private val routeOptionsProvider: RouteOptionsProvider = mockk()

    private val routeOptionsResultSuccess: RouteOptionsProvider.RouteOptionsResult.Success = mockk()
    private val routeOptionsResultSuccessRouteOptions: RouteOptions = mockk()
    private val fasterRouteDetector: FasterRouteDetector = mockk()

    private val fasterRouteController = FasterRouteController(directionsSession, tripSession, routeOptionsProvider, fasterRouteDetector, logger)

    @Before
    fun setup() {
        every { routeOptionsResultSuccess.routeOptions } returns routeOptionsResultSuccessRouteOptions

        every { directionsSession.getRouteOptions() } returns mockk()
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()
        every { tripSession.getRouteProgress() } returns mockk()
    }

    @Test(expected = IllegalStateException::class)
    fun `should throw when interval is less than 2 minutes`() = coroutineRule.runBlockingTest {
        val fasterRouteObserver: FasterRouteObserver = mockk {
            every { restartAfterMillis() } returns TimeUnit.MINUTES.toMillis(1)
            every { onFasterRoute(any(), any(), any()) } returns Unit
        }

        fasterRouteController.attach(fasterRouteObserver)
    }

    @Test
    fun `should not throw when interval is 2 or more minutes`() = coroutineRule.runBlockingTest {
        val fasterRouteObserver: FasterRouteObserver = mockk {
            every { restartAfterMillis() } returns TimeUnit.MINUTES.toMillis(2)
            every { onFasterRoute(any(), any(), any()) } returns Unit
        }

        fasterRouteController.attach(fasterRouteObserver)
    }

    @Test
    fun `should request every 5 minutes`() = coroutineRule.runBlockingTest {
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        every { directionsSession.routes } returns listOf(
            mockk {
                every { routeIndex() } returns "0"
                every { duration() } returns 1727.228
            }
        )
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(25))
        fasterRouteController.stop()

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 5) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun `should only request with a route`() = coroutineRule.runBlockingTest {
        every { directionsSession.routes } returns emptyList()
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        fasterRouteController.stop()

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun `should notify observer of a faster route`() = coroutineRule.runBlockingTest {
        coEvery { fasterRouteDetector.isRouteFaster(any(), any()) } returns true
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        val routes = listOf<DirectionsRoute>(mockk {
                every { routeIndex() } returns "0"
            })
        routesRequestCallbacks.captured.onRoutesReady(routes)

        verify(exactly = 1) { fasterRouteObserver.onFasterRoute(currentRoute, routes, true) }

        fasterRouteController.stop()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `should notify observer if current route is fastest`() = coroutineRule.runBlockingTest {
        coEvery { fasterRouteDetector.isRouteFaster(any(), any()) } returns false
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        val routes = listOf<DirectionsRoute>(mockk {
            every { routeIndex() } returns "0"
        })
        routesRequestCallbacks.captured.onRoutesReady(routes)

        verify(exactly = 1) { fasterRouteObserver.onFasterRoute(currentRoute, routes, false) }

        fasterRouteController.stop()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }

    private fun mockRouteOptionsProvider(routeOptionsResult: RouteOptionsProvider.RouteOptionsResult) {
        every { routeOptionsProvider.update(any(), any(), any()) } returns routeOptionsResult
    }
}
