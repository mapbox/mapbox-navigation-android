package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class RouteAlternativesControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val navigator: MapboxNativeNavigator = mockk()
    private val directionsSession: DirectionsSession = mockk() {
        every { cancelRouteRequest(any()) } just Runs
    }
    private val tripSession: TripSession = mockk {
        every { getRouteProgress() } returns mockk()
    }
    private val routeAlternativesObserver: RouteAlternativesObserver = mockk {
        every { onRouteAlternatives(any(), any(), any()) } returns Unit
    }
    private val routesRequestCallbacks = slot<RouterCallback>()
    private val routeOptionsUpdater: RouteOptionsUpdater = mockk()

    private val routeOptionsResultSuccess: RouteOptionsUpdater.RouteOptionsResult.Success = mockk()
    private val routeOptionsResultSuccessRouteOptions: RouteOptions = mockk()

    private fun mockController(
        options: RouteAlternativesOptions = RouteAlternativesOptions.Builder().build()
    ) = RouteAlternativesController(
        options,
        navigator,
        directionsSession,
        tripSession,
        routeOptionsUpdater
    )

    @Before
    fun setup() {
        every {
            routeOptionsResultSuccess.routeOptions
        } returns routeOptionsResultSuccessRouteOptions

        every { directionsSession.getPrimaryRouteOptions() } returns mockk()
        every {
            directionsSession.requestRoutes(
                any(),
                capture(routesRequestCallbacks)
            )
        } returns 1L
        every { tripSession.getRouteProgress() } returns mockk()
        mockkObject(LoggerProvider) {
            every { LoggerProvider.logger } returns mockk(relaxed = true)
        }
    }

    @After
    fun teardown() {
        unmockkObject(LoggerProvider)
    }

    @Test
    fun `should request every 5 minutes by default`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
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

        val controller = mockController()
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(25))
        controller.unregister(routeAlternativesObserver)

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 5) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun `should request every 2 minutes from options`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
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

        val controller = mockController(
            RouteAlternativesOptions.Builder()
                .intervalMillis(TimeUnit.MINUTES.toMillis(2))
                .build()
        )
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(25))
        controller.unregister(routeAlternativesObserver)

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 12) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun `interrupting a request will also restart the interval`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
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

        val controller = mockController(
            RouteAlternativesOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(10))
                .build()
        )

        // Advance time to almost make a request, but cancel before. Prove that
        // a new timer is started because there would be 2 calls otherwise.
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.SECONDS.toMillis(9))
        controller.interrupt()
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.SECONDS.toMillis(19))
        controller.unregister(routeAlternativesObserver)

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 1) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun `should only request with a route`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        every { directionsSession.routes } returns emptyList()
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }

        val controller = mockController()
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(25))
        controller.unregister(routeAlternativesObserver)

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun `should not request when trip session is stopped`() {
        every { tripSession.getState() } returns TripSessionState.STOPPED
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

        val controller = mockController()
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(25))
        controller.unregister(routeAlternativesObserver)

        coroutineRule.testDispatcher.cleanupTestCoroutines()
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun `should not request when interrupted and there are no observers`() =
        coroutineRule.runBlockingTest {
            every { tripSession.getState() } returns TripSessionState.STARTED
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

            val controller = mockController()
            controller.interrupt()
            coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(5))

            verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }
            coroutineRule.testDispatcher.cleanupTestCoroutines()
        }

    @Test
    fun `should notify observer of an alternative`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
        coEvery { navigator.isDifferentRoute(any()) } returns true
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every {
            directionsSession.requestRoutes(
                any(),
                capture(routesRequestCallbacks)
            )
        } returns 1L

        val controller = mockController()
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        val routes = listOf<DirectionsRoute>(
            mockk {
                every { routeIndex() } returns "0"
            }
        )
        val origin = mockk<RouterOrigin>()
        routesRequestCallbacks.captured.onRoutesReady(routes, origin)

        verify(exactly = 1) {
            routeAlternativesObserver.onRouteAlternatives(any(), routes, origin)
        }

        controller.unregisterAll()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `should filter routes that are not different`() = coroutineRule.runBlockingTest {
        every { tripSession.getState() } returns TripSessionState.STARTED
        coEvery { navigator.isDifferentRoute(any()) } returns false
        mockRouteOptionsProvider(routeOptionsResultSuccess)
        val currentRoute: DirectionsRoute = mockk {
            every { routeIndex() } returns "0"
        }
        every { directionsSession.routes } returns listOf(currentRoute)
        every { tripSession.getEnhancedLocation() } returns mockk {
            every { latitude } returns -33.874308
            every { longitude } returns 151.206087
        }
        every {
            directionsSession.requestRoutes(
                any(),
                capture(routesRequestCallbacks)
            )
        } returns 1L
        val origin = mockk<RouterOrigin>()

        val controller = mockController()
        controller.register(routeAlternativesObserver)
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.MINUTES.toMillis(6))
        routesRequestCallbacks.captured.onRoutesReady(emptyList(), origin)

        verify(exactly = 1) {
            routeAlternativesObserver.onRouteAlternatives(any(), emptyList(), origin)
        }

        controller.unregisterAll()
        coroutineRule.testDispatcher.cleanupTestCoroutines()
    }

    private fun mockRouteOptionsProvider(
        routeOptionsResult: RouteOptionsUpdater.RouteOptionsResult
    ) {
        every { routeOptionsUpdater.update(any(), any(), any()) } returns routeOptionsResult
    }
}
