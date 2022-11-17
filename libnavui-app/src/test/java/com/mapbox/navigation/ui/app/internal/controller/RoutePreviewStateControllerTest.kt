package com.mapbox.navigation.ui.app.internal.controller

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.testing.TestStore
import com.mapbox.navigation.ui.app.testing.TestingUtil.makeLocationMatcherResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.LOLLIPOP])
internal class RoutePreviewStateControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: RoutePreviewStateController

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mockkObject(MapboxNavigationApp)
        mapboxNavigation = mockMapboxNavigation(context)
        store = spyk(TestStore())
        store.setState(
            State(
                destination = Destination(Point.fromLngLat(11.0, 22.0)),
                location = makeLocationMatcherResult(33.0, 44.0, 0f)
            )
        )
        sut = RoutePreviewStateController(store, RouteOptionsProvider())
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteSaga - should cancel previous route requests when SetDestination action is dispatched`() {
        every {
            mapboxNavigation.requestRoutes(any(), ofType(NavigationRouterCallback::class))
        } returnsMany listOf(1L, 2L, 3L)

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        store.dispatch(DestinationAction.SetDestination(null))

        verify { mapboxNavigation.cancelRouteRequest(1L) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteSaga - should cancel previous route requests when another FetchRoute action is dispatched`() {
        every {
            mapboxNavigation.requestRoutes(any(), ofType(NavigationRouterCallback::class))
        } returnsMany listOf(1L, 2L, 3L)

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        store.dispatch(RoutePreviewAction.FetchRoute)

        verify { mapboxNavigation.cancelRouteRequest(1L) }
    }

    @Test
    fun `fetchRouteSaga - should dispatch StartedFetchRequest when fetching routes`() {
        every {
            mapboxNavigation.requestRoutes(any(), ofType(NavigationRouterCallback::class))
        } returns 1L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)

        assertActionDispatched(RoutePreviewAction.StartedFetchRequest(1L))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteSaga - should use MapboxNavigation to fetch routes and set Fetching state`() {
        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)

        assertTrue(store.state.value.previewRoutes is RoutePreviewState.Fetching)
        verify { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) }
    }

    @Test
    fun `fetchRouteSaga - after requestRoutes success should update to Ready`() {
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        callbackSlot.captured.onRoutesReady(routes, mockk())

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Ready
        assertNotNull(readyState)
        assertEquals(readyState?.routes, routes)
    }

    @Test
    fun `fetchRouteSaga - will result in Failed state on fetch routes failure`() {
        val reasons = listOf<RouterFailure>(mockk())
        val routeOptions = mockk<RouteOptions>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        callbackSlot.captured.onFailure(reasons, routeOptions)

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Failed
        assertNotNull(readyState)
        assertEquals(readyState?.reasons, reasons)
        assertEquals(readyState?.routeOptions, routeOptions)
    }

    @Test
    fun `fetchRouteSaga - will result in Canceled state on fetch request cancellation`() {
        val routeOptions = mockk<RouteOptions>()
        val routerOrigin = mockk<RouterOrigin>()
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        callbackSlot.captured.onCanceled(routeOptions, routerOrigin)

        val readyState = store.state.value.previewRoutes as? RoutePreviewState.Canceled
        assertNotNull(readyState)
        assertEquals(readyState?.routeOptions, routeOptions)
        assertEquals(readyState?.routerOrigin, routerOrigin)
    }

    @Test
    fun `process - StartedFetchRequest action should save requestId in the store`() {
        sut.onAttached(mapboxNavigation)

        store.dispatch(RoutePreviewAction.StartedFetchRequest(1L))

        val requestId = (store.state.value.previewRoutes as RoutePreviewState.Fetching).requestId
        assertEquals(1L, requestId)
    }

    @Test
    fun `onDetached should canceled fetch request`() {
        every { mapboxNavigation.requestRoutes(any(), any<NavigationRouterCallback>()) } answers {
            123L
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRoute)
        sut.onDetached(mapboxNavigation)

        verify { mapboxNavigation.cancelRouteRequest(123L) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndShowRoutePreview should dispatch FetchRoute and update NavigationState on RoutePreviewState_Ready`() {
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndShowRoutePreview)
        callbackSlot.captured.onRoutesReady(routes, mockk())

        assertTrue(store.state.value.previewRoutes is RoutePreviewState.Ready)
        assertActionDispatched(RoutePreviewAction.FetchRoute)
        assertActionDispatched(NavigationStateAction.Update(NavigationState.RoutePreview))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndShowRoutePreview should NOT dispatch FetchRoute when already in RoutePreviewState_Ready state`() {
        val routes = listOf<NavigationRoute>(mockk())
        store.updateState {
            it.copy(previewRoutes = RoutePreviewState.Ready(routes))
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndShowRoutePreview)

        assertActionNotDispatched(RoutePreviewAction.FetchRoute)
        assertActionDispatched(NavigationStateAction.Update(NavigationState.RoutePreview))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndShowRoutePreview should ABORT when in RoutePreviewState_Fetching state`() {
        store.updateState {
            it.copy(previewRoutes = RoutePreviewState.Fetching(123))
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndShowRoutePreview)

        assertActionNotDispatched(RoutePreviewAction.FetchRoute)
        assertActionNotDispatched(NavigationStateAction.Update(NavigationState.RoutePreview))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndStartActiveNavigation should dispatch FetchRoute and update NavigationState on RoutePreviewState_Ready`() {
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndStartActiveNavigation)
        callbackSlot.captured.onRoutesReady(routes, mockk())

        assertTrue(store.state.value.previewRoutes is RoutePreviewState.Ready)
        assertActionDispatched(RoutePreviewAction.FetchRoute)
        assertActionDispatched(RoutesAction.SetRoutes(routes))
        assertActionDispatched(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndStartActiveNavigation should NOT dispatch FetchRoute when already in RoutePreviewState_Ready state`() {
        val routes = listOf<NavigationRoute>(mockk())
        store.updateState {
            it.copy(previewRoutes = RoutePreviewState.Ready(routes))
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndStartActiveNavigation)

        assertActionNotDispatched(RoutePreviewAction.FetchRoute)
        assertActionDispatched(RoutesAction.SetRoutes(routes))
        assertActionDispatched(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on FetchRouteAndStartActiveNavigation should ABORT when in RoutePreviewState_Fetching state`() {
        store.updateState {
            it.copy(previewRoutes = RoutePreviewState.Fetching(123))
        }

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndStartActiveNavigation)

        assertActionNotDispatched(RoutePreviewAction.FetchRoute)
        assertActionNotDispatched(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `fetchRouteAndMoveToNavigationStateSaga - on should ABORT previous work when NavigationStateAction_Update is dispatched`() {
        val routes = listOf<NavigationRoute>(mockk())
        val callbackSlot = slot<NavigationRouterCallback>()
        every { mapboxNavigation.requestRoutes(any(), capture(callbackSlot)) } returns 123L

        sut.onAttached(mapboxNavigation)
        store.dispatch(RoutePreviewAction.FetchRouteAndStartActiveNavigation)
        assertActionDispatched(RoutePreviewAction.FetchRoute)

        // simulate NavigationState update while fetch request is in-flight
        store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
        // finish route request
        callbackSlot.captured.onRoutesReady(routes, mockk())
        assertActionNotDispatched(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }

    private fun mockMapboxNavigation(context: Context): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { navigationOptions } returns NavigationOptions.Builder(context).build()
        }
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }

    private fun assertActionDispatched(action: Action) =
        assertTrue("expected $action action to be dispatched", store.didDispatchAction(action))

    private fun assertActionNotDispatched(action: Action) =
        assertFalse("unexpected $action action was dispatched", store.didDispatchAction(action))
}
