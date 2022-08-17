package com.mapbox.navigation.dropin.internal

import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.ui.app.internal.Reducer
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewApiTest {

    private lateinit var sut: MapboxNavigationViewApi

    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        testStore = spyk(TestStore())
        // registering simple reducer that copies destination and routes values
        testStore.register(
            Reducer { state, action ->
                when (action) {
                    is DestinationAction.SetDestination ->
                        state.copy(destination = action.destination)
                    is RoutePreviewAction.Ready ->
                        state.copy(previewRoutes = RoutePreviewState.Ready(action.routes))
                    is RoutesAction.SetRoutes ->
                        state.copy(routes = action.routes)
                    else -> state
                }
            }
        )
        sut = MapboxNavigationViewApi(testStore)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startFreeDrive should clear routes and preview routes and set FreeDrive NavigationState`() {
        sut.startFreeDrive()

        verify { testStore.dispatch(DestinationAction.SetDestination(null)) }
        verify { testStore.dispatch(RoutesAction.SetRoutes(emptyList())) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(emptyList())) }
        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive)) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startDestinationPreview should set new destination and set DestinationPreview NavigationState`() {
        val point = Point.fromLngLat(22.0, 33.0)

        sut.startDestinationPreview(point)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.DestinationPreview))
        }
    }

    @Test
    fun `startRoutePreview should set RoutePreview NavigationState`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        val result = sut.startRoutePreview()

        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview)) }
        assertTrue(result.isValue)
    }

    @Test
    fun `startRoutePreview should return an Error if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        val error = sut.startRoutePreview().error

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `startRoutePreview should fail with IllegalStateException if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }

        val result = sut.startRoutePreview()

        assertTrue(result.error is IllegalStateException)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                waypoint(Point.fromLngLat(1.0, 2.0)),
                waypoint(Point.fromLngLat(2.0, 3.0)),
                waypoint(point),
            )
        )

        val result = sut.startRoutePreview(routes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(routes)) }
        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview)) }
        assertTrue(result.isValue)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should fail with IllegalArgumentException if preview routes are empty`() {
        val result = sut.startRoutePreview(emptyList())

        assertTrue(result.error is IllegalArgumentException)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startNavigation should set preview routes as routes and set ActiveNavigation NavigationState`() {
        val routes = listOf<NavigationRoute>(mockk())
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(routes)
            )
        }

        val result = sut.startNavigation()

        verify { testStore.dispatch(RoutesAction.SetRoutes(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
        assertTrue(result.isValue)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startNavigation should fail with IllegalStateException if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        val result = sut.startNavigation()

        assertTrue(result.error is IllegalStateException)
    }

    @Test
    fun `startNavigation should fail with IllegalStateException if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }

        val result = sut.startNavigation()

        assertTrue(result.error is IllegalStateException)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startNavigation should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                waypoint(Point.fromLngLat(1.0, 2.0)),
                waypoint(Point.fromLngLat(2.0, 3.0)),
                waypoint(point),
            )
        )

        val result = sut.startNavigation(routes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
        assertTrue(result.isValue)
    }

    @Test
    fun `startNavigation should fail with IllegalArgumentException if preview routes are empty`() {
        val result = sut.startNavigation(emptyList())

        assertTrue(result.error is IllegalArgumentException)
    }

    @Test
    fun `startArrival should set Arrival NavigationState`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                routes = listOf(mockk())
            )
        }

        val result = sut.startArrival()

        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.Arrival)) }
        assertTrue(result.isValue)
    }

    @Test
    fun `startArrival should fail with IllegalStateException if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                routes = listOf(mockk())
            )
        }

        val result = sut.startArrival()

        assertTrue(result.error is IllegalStateException)
    }

    @Test
    fun `startArrival should fail with IllegalStateException if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = null,
                routes = emptyList()
            )
        }

        val result = sut.startArrival()

        assertTrue(result.error is IllegalStateException)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startArrival should set destination, routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                waypoint(Point.fromLngLat(1.0, 2.0)),
                waypoint(Point.fromLngLat(2.0, 3.0)),
                waypoint(point),
            )
        )

        val result = sut.startArrival(routes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(routes)) }
        verify { testStore.dispatch(RoutesAction.SetRoutes(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
        }
        assertTrue(result.isValue)
    }

    @Test
    fun `startArrival should fail with IllegalArgumentException if preview routes are empty`() {
        val result = sut.startArrival(emptyList())

        assertTrue(result.error is IllegalArgumentException)
    }

    @Test
    fun `isReplayEnabled should return true when replay trip session is enabled `() {
        assertFalse(sut.isReplayEnabled())
        testStore.updateState {
            it.copy(
                tripSession = it.tripSession.copy(isReplayEnabled = true)
            )
        }
        assertTrue(sut.isReplayEnabled())
    }

    @Test
    fun `routeReplayEnabled should dispatch EnableReplayTripSession`() {
        sut.routeReplayEnabled(true)
        verify { testStore.dispatch(TripSessionStarterAction.EnableReplayTripSession) }
    }

    @Test
    fun `routeReplayEnabled should dispatch EnableTripSession`() {
        sut.routeReplayEnabled(false)
        verify { testStore.dispatch(TripSessionStarterAction.EnableTripSession) }
    }

    private fun navigationRoute(vararg waypoints: DirectionsWaypoint): NavigationRoute {
        return mockk {
            every { directionsResponse } returns mockk {
                every { waypoints() } returns waypoints.toList()
            }
        }
    }

    private fun waypoint(point: Point): DirectionsWaypoint {
        return mockk {
            every { location() } returns point
        }
    }
}
