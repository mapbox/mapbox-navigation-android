package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class RoutesViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var navigationStateFlow: MutableStateFlow<NavigationState>
    lateinit var destinationStateFlow: MutableStateFlow<DestinationState>
    lateinit var navigationStateViewModel: NavigationStateViewModel
    lateinit var locationViewModel: LocationViewModel
    lateinit var destinationViewModel: DestinationViewModel

    lateinit var sut: RoutesViewModel

    private val currentLoc = Point.fromLngLat(10.0, 11.0)

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        navigationStateFlow = MutableStateFlow(NavigationState.FreeDrive)
        destinationStateFlow = MutableStateFlow(DestinationState())
        navigationStateViewModel = mockk {
            every { state } returns navigationStateFlow
        }
        locationViewModel = mockk(relaxed = true) {
            every { lastPoint } returns currentLoc
        }
        destinationViewModel = mockk {
            every { state } returns destinationStateFlow
        }
        sut = RoutesViewModel(
            navigationStateViewModel,
            locationViewModel,
            destinationViewModel,
            RoutesState()
        )
    }

    @After
    fun cleanUp() {
        unmockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
    }

    @Test
    fun `onAttached calls setRoutes on mapbox navigation via setRouteRequests flow`() {
        val mockRoute = mockk<List<NavigationRoute>>()
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)
        sut.invoke(RoutesAction.SetRoutes(mockRoute))

        verify { mapboxNavigation.setNavigationRoutes(mockRoute) }
    }

    @Test
    fun `onAttached calls requestRoutes on mapbox navigation via routeRequests flow`() =
        runBlockingTest {
            val points = listOf(
                Point.fromLngLat(33.0, 44.0),
                Point.fromLngLat(33.1, 44.1)
            )
            val mapboxNavigation = mockMapboxNavigation()

            sut.onAttached(mapboxNavigation)
            sut.invoke(RoutesAction.FetchPoints(points))

            verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
        }

    @Test
    fun `onAttached calls requestRoutes on mapbox navigation via routeOptionsRequests flow`() {
        val routeOptions = mockk<RouteOptions>()
        val mapboxNavigation = mockMapboxNavigation()

        sut.onAttached(mapboxNavigation)
        sut.invoke(RoutesAction.FetchOptions(routeOptions))

        verify { mapboxNavigation.requestRoutes(routeOptions, any<NavigationRouterCallback>()) }
    }

    @Test
    fun `onRoutesReady route requests set routes on mapbox navigation`() {
        val route1 = mockk<NavigationRoute>()
        val route2 = mockk<NavigationRoute>()
        val routes = listOf(route1, route2)
        val routeOptions = mockk<RouteOptions>()
        val mapboxNavigation = mockMapboxNavigation()
        val callbackSlot = slot<NavigationRouterCallback>()
        val setRoutesSlot = slot<List<NavigationRoute>>()
        sut.onAttached(mapboxNavigation)
        sut.invoke(RoutesAction.FetchOptions(routeOptions))

        verify { mapboxNavigation.requestRoutes(routeOptions, capture(callbackSlot)) }

        callbackSlot.captured.onRoutesReady(routes, mockk())

        verify { mapboxNavigation.setNavigationRoutes(capture(setRoutesSlot)) }
        assertEquals(route1, setRoutesSlot.captured[0])
        assertEquals(route2, setRoutesSlot.captured[1])
    }

    @Test
    fun `onDetached cancels mapbox navigation route request`() {
        val requestCode = 333L
        val routeOptions = mockk<RouteOptions>()
        val mapboxNavigation = mockMapboxNavigation()
        every {
            mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>())
        } returns requestCode

        sut.onAttached(mapboxNavigation)
        sut.invoke(RoutesAction.FetchOptions(routeOptions))
        sut.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(requestCode) }
    }

    @Test
    fun `route fetch with default route options`() {
        val points = listOf(
            Point.fromLngLat(33.0, 44.0),
            Point.fromLngLat(33.1, 44.1)
        )
        val mapboxNavigation = mockMapboxNavigation()
        every { mapboxNavigation.getZLevel() } returns 9
        val optionsSlot = slot<RouteOptions>()

        sut.onAttached(mapboxNavigation)
        sut.invoke(RoutesAction.FetchPoints(points))

        verify {
            mapboxNavigation.requestRoutes(capture(optionsSlot), any<NavigationRouterCallback>())
        }
        assertEquals(points.first(), optionsSlot.captured.coordinatesList().first())
        assertEquals(points[1], optionsSlot.captured.coordinatesList()[1])
        assertTrue(optionsSlot.captured.alternatives()!!)
        assertEquals(9, optionsSlot.captured.layersList()!!.first())
        assertEquals("en", optionsSlot.captured.language())
        assertEquals(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, optionsSlot.captured.profile())
        assertEquals(DirectionsCriteria.OVERVIEW_FULL, optionsSlot.captured.overview())
        assertTrue(optionsSlot.captured.steps()!!)
        assertTrue(optionsSlot.captured.roundaboutExits()!!)
        assertTrue(optionsSlot.captured.voiceInstructions()!!)
        assertTrue(optionsSlot.captured.bannerInstructions()!!)
        assertEquals(
            "congestion_numeric,maxspeed,closure,speed,duration,distance",
            optionsSlot.captured.annotations()
        )
    }

    @Test
    fun `should fetch and set new route on Destination change when in RoutePreview state`() =
        coroutineRule.runBlockingTest {
            navigationStateFlow.value = NavigationState.RoutePreview
            val destination = Destination(Point.fromLngLat(1.0, 2.0))
            val mapboxNavigation = mockMapboxNavigation()

            sut.onAttached(mapboxNavigation)
            destinationStateFlow.value = DestinationState(destination)

            verifyFetchAndSet(
                mapboxNavigation = mapboxNavigation,
                requestCoordinates = listOf(currentLoc, destination.point),
                responseRoutes = listOf(mockk())
            )
        }

    @Test
    fun `should fetch and set new route on FetchAndSet action`() =
        coroutineRule.runBlockingTest {
            val destination = Destination(Point.fromLngLat(1.0, 2.0))
            destinationStateFlow.value = DestinationState(destination)
            val mapboxNavigation = mockMapboxNavigation()

            sut.onAttached(mapboxNavigation)
            sut.invoke(RoutesAction.FetchAndSetRoute)

            verifyFetchAndSet(
                mapboxNavigation = mapboxNavigation,
                requestCoordinates = listOf(currentLoc, destination.point),
                responseRoutes = listOf(mockk())
            )
        }

    @Test
    fun `should set navigationStarted to TRUE on DidStartNavigation`() =
        coroutineRule.runBlockingTest {
            val mapboxNavigation = mockMapboxNavigation()

            sut.onAttached(mapboxNavigation)
            sut.invoke(RoutesAction.DidStartNavigation)

            assertTrue(sut.state.value.navigationStarted)
        }

    @Test
    fun `should set navigationStarted to FALSE on DidStartNavigation`() =
        coroutineRule.runBlockingTest {
            val initialState = RoutesState(true)
            sut = RoutesViewModel(
                navigationStateViewModel,
                locationViewModel,
                destinationViewModel,
                initialState
            )
            val mapboxNavigation = mockMapboxNavigation()

            sut.onAttached(mapboxNavigation)
            sut.invoke(RoutesAction.DidStopNavigation)

            assertFalse(sut.state.value.navigationStarted)
        }

    private fun verifyFetchAndSet(
        mapboxNavigation: MapboxNavigation,
        requestCoordinates: List<Point>,
        responseRoutes: List<NavigationRoute>
    ) {
        val optionsSlot = slot<RouteOptions>()
        val callbackSlot = slot<NavigationRouterCallback>()
        verify {
            mapboxNavigation.requestRoutes(capture(optionsSlot), capture(callbackSlot))
        }

        assertEquals(requestCoordinates, optionsSlot.captured.coordinatesList())

        callbackSlot.captured.onRoutesReady(responseRoutes, mockk())
        verify { mapboxNavigation.setNavigationRoutes(responseRoutes) }
    }

    private fun mockMapboxNavigation() = mockk<MapboxNavigation>(relaxed = true) {
        every { getZLevel() } returns 9
        every { navigationOptions } returns mockk {
            every { navigationOptions } returns mockk {
                every { applicationContext } returns mockk {
                    every { inferDeviceLocale() } returns Locale.ENGLISH
                    every { resources } returns mockk {
                        every { configuration } returns mockk()
                    }
                }
            }
        }
    }
}
