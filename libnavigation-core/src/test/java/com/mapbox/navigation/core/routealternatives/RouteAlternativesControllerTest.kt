package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigator.RouteAlternativesControllerInterface
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RouteAlternativesControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val controllerInterface: RouteAlternativesControllerInterface = mockk(relaxed = true)
    private val navigator: MapboxNativeNavigator = mockk {
        every { createRouteAlternativesController() } returns controllerInterface
    }
    private val directionsSession: DirectionsSession = mockk(relaxed = true)
    private val tripSession: TripSession = mockk(relaxed = true)

    private fun routeAlternativesController(
        options: RouteAlternativesOptions = RouteAlternativesOptions.Builder().build()
    ) = RouteAlternativesController(
        options,
        navigator,
        directionsSession,
        tripSession,
    )

    @Test
    fun `should set refreshIntervalSeconds from options`() {
        // Capture the route options set
        val nativeOptions = slot<com.mapbox.navigator.RouteAlternativesOptions>()
        every { controllerInterface.setRouteAlternativesOptions(capture(nativeOptions)) } just runs

        // Construct a native route alternatives interface
        routeAlternativesController(
            options = RouteAlternativesOptions.Builder()
                .intervalMillis(59_135L)
                .build()
        )

        // Verify the conversion was accurate
        assertEquals(
            59.135,
            nativeOptions.captured.refreshIntervalSeconds,
            0.001
        )
    }

    @Test
    fun `should add a single nav-native observer when registering listeners`() {
        val routeAlternativesController = routeAlternativesController()

        routeAlternativesController.register(mockk(relaxed = true))
        routeAlternativesController.register(mockk(relaxed = true))

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 0) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeObserver from nav-native interface when all observers are removed`() {
        val routeAlternativesController = routeAlternativesController()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        routeAlternativesController.unregister(firstObserver)
        routeAlternativesController.unregister(secondObserver)

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeAllObservers from nav-native interface when unregisterAll is called`() {
        val routeAlternativesController = routeAlternativesController()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        routeAlternativesController.unregisterAll()

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeAllObservers() }
    }

    @Test
    fun `should broadcast alternative routes changes from nav-native`() {
        val routeAlternativesController = routeAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true)
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.txt"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    every { route } returns alternativeRouteJson
                }
            )
        )

        verify(exactly = 1) { firstObserver.onRouteAlternatives(any(), any(), any()) }
        verify(exactly = 1) { secondObserver.onRouteAlternatives(any(), any(), any()) }
    }

    @Test
    fun `should broadcast current route with alternative`() {
        val routeAlternativesController = routeAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true) {
                every { duration() } returns 200.0
            }
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.txt"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    every { route } returns alternativeRouteJson
                }
            )
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<DirectionsRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            firstObserver.onRouteAlternatives(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals(200.0, routeProgressSlot.captured.route.duration(), 0.01)
        assertEquals(221.796, alternativesSlot.captured[0].duration(), 0.01)
        assertEquals(RouterOrigin.Onboard, routerOriginSlot.captured)
    }

    @Test
    fun `should set route with the new alternatives`() {
        val routeAlternativesController = routeAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true)
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.txt"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    every { route } returns alternativeRouteJson
                }
            )
        )

        val routesSetCapture = slot<List<DirectionsRoute>>()
        verify(exactly = 1) {
            directionsSession.setRoutes(
                capture(routesSetCapture),
                0,
                RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE
            )
        }
        assertEquals(2, routesSetCapture.captured.size)
        assertEquals(221.796, routesSetCapture.captured[1].duration(), 0.001)
    }

    @Test
    fun `should set alternative RouteOptions to primary RouteOptions`() {
        val originalCoordinates = "-122.270375,37.801429;-122.271496, 37.799063"
        val routeAlternativesController = routeAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk {
                every { routeOptions() } returns mockk {
                    every { coordinates() } returns originalCoordinates
                }
            }
        }
        val routesSetCapture = slot<List<DirectionsRoute>>()
        every { directionsSession.setRoutes(capture(routesSetCapture), any(), any()) } just runs

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.txt"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    every { route } returns alternativeRouteJson
                }
            )
        )

        assertEquals(
            originalCoordinates, routesSetCapture.captured[0].routeOptions()?.coordinates()
        )
        assertEquals(
            originalCoordinates, routesSetCapture.captured[1].routeOptions()?.coordinates()
        )
    }
}
