package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.core.directions.session.AdjustedRouteOptionsProvider
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class FasterRouteControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val directionsSession: DirectionsSession = mockk()
    private val tripSession: TripSession = mockk()
    private val fasterRouteObserver: FasterRouteObserver = mockk {
        every { restartAfterMillis() } returns TimeUnit.MINUTES.toMillis(1)
        every { onFasterRoute(any(), any(), any()) } returns Unit
    }
    private val routesRequestCallbacks = slot<RoutesRequestCallback>()

    private val logger: Logger = mockk()
    private val fasterRouteController = FasterRouteController(directionsSession, tripSession, logger)

    @Before
    fun setup() {
        mockkObject(AdjustedRouteOptionsProvider)
        every { AdjustedRouteOptionsProvider.getRouteOptions(any(), any(), any()) } returns mockk()

        every { directionsSession.getRouteOptions() } returns mockk()
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()
        every { tripSession.getRouteProgress() } returns mockk()
    }

    @After
    fun teardown() {
        unmockkObject(AdjustedRouteOptionsProvider)
    }

    @Test
    fun `should request every minute`() = coroutineRule.runBlockingTest {
        every { directionsSession.routes } returns listOf(
            mockk {
                every { routeIndex() } returns "0"
                every { duration() } returns 727.228
            }
        )
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(4))
        fasterRouteController.stop()

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 4) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun `should only request with a route`() = coroutineRule.runBlockingTest {
        every { directionsSession.routes } returns emptyList()
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(4))
        fasterRouteController.stop()

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 0) { directionsSession.requestFasterRoute(any(), any()) }
    }

    @Test
    fun `should notify observer of a faster route`() = coroutineRule.runBlockingTest {
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
            every { duration() } returns 801.332
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every { tripSession.getRouteProgress() } returns mockk {
            every { durationRemaining() } returns 601.334
        }
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(2))
        val routes = listOf<DirectionsRoute>(mockk {
                every { routeIndex() } returns "0"
                every { duration() } returns 351.013
            })
        routesRequestCallbacks.captured.onRoutesReady(routes)

        verify(exactly = 1) { fasterRouteObserver.onFasterRoute(currentRoute, routes[0], true) }

        fasterRouteController.stop()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `should notify observer if current route is fastest`() = coroutineRule.runBlockingTest {
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
            every { duration() } returns 801.332
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every { tripSession.getRouteProgress() } returns mockk {
            every { durationRemaining() } returns 751.334
        }
        every { directionsSession.requestFasterRoute(any(), capture(routesRequestCallbacks)) } returns mockk()

        fasterRouteController.attach(fasterRouteObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(2))
        val routes = listOf<DirectionsRoute>(mockk {
            every { routeIndex() } returns "0"
            every { duration() } returns 951.013
        })
        routesRequestCallbacks.captured.onRoutesReady(routes)

        verify(exactly = 1) { fasterRouteObserver.onFasterRoute(currentRoute, routes[0], false) }

        fasterRouteController.stop()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }
}
