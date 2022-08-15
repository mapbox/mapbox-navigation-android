package com.mapbox.navigation.dropin.internal

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.startNavigation
import com.mapbox.navigation.dropin.startRoutePreview
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.ui.app.internal.Reducer
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewApiTest {

    private lateinit var sut: MapboxNavigationViewApi

    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        testStore = spyk(TestStore())
        // registering simple reducer that copies Destination and RoutePreviewState values
        testStore.register(
            Reducer { state, action ->
                when (action) {
                    is DestinationAction.SetDestination ->
                        state.copy(destination = action.destination)
                    is RoutePreviewAction.Ready ->
                        state.copy(previewRoutes = RoutePreviewState.Ready(action.routes))
                    else -> state
                }
            }
        )
        sut = MapboxNavigationViewApi(testStore)
    }

    @Test
    fun `startFreeDrive should clear routes and preview routes and set FreeDrive NavigationState`() {
        sut.startFreeDrive()

        verify { testStore.dispatch(RoutesAction.SetRoutes(emptyList())) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(emptyList())) }
        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive)) }
    }

    @Test
    fun `startRoutePreview should set RoutePreview NavigationState`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        sut.startRoutePreview()

        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview)) }
    }

    @Test
    fun `startRoutePreview should throw IllegalStateException if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        try {
            sut.startRoutePreview()
            fail("expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            // pass
        }
    }

    @Test
    fun `startRoutePreview should throw IllegalStateException if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }
        try {
            sut.startRoutePreview()
            fail("expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            // pass
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val previewRoutes = listOf<NavigationRoute>(mockk())

        sut.startRoutePreview(point, previewRoutes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(previewRoutes)) }
        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview)) }
    }

    @Test
    fun `startRoutePreview should throw IllegalArgumentException if preview routes are empty`() {
        val point = Point.fromLngLat(11.0, 22.0)
        try {
            sut.startRoutePreview(point, emptyList())
            fail("expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            // pass
        }
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

        sut.startNavigation()

        verify { testStore.dispatch(RoutesAction.SetRoutes(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
    }

    @Test
    fun `startNavigation should throw IllegalStateException if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        try {
            sut.startNavigation()
            fail("expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            // pass
        }
    }

    @Test
    fun `startNavigation should throw IllegalStateException if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }
        try {
            sut.startNavigation()
            fail("expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            // pass
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startNavigation should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val previewRoutes = listOf<NavigationRoute>(mockk())

        sut.startNavigation(point, previewRoutes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(previewRoutes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
    }

    @Test
    fun `startNavigation should throw IllegalArgumentException if preview routes are empty`() {
        val point = Point.fromLngLat(11.0, 22.0)
        try {
            sut.startNavigation(point, emptyList())
            fail("expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    fun `startArrival should set Arrival NavigationState`() {
        sut.startArrival()

        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.Arrival)) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `endNavigation should clear Destination and Route data and set FreeDrive NavigationState`() {
        sut.endNavigation()

        verify { testStore.dispatch(DestinationAction.SetDestination(null)) }
        verify { testStore.dispatch(RoutesAction.SetRoutes(emptyList())) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(emptyList())) }
        verify { testStore.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive)) }
    }
}
