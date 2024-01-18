package com.mapbox.navigation.dropin.navigationview

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.NavigationViewApiErrorTypes
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.ui.app.internal.Reducer
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapboxNavigationViewApiTest {

    private lateinit var sut: MapboxNavigationViewApi

    private lateinit var testStore: TestStore
    private var audioGuidance: MapboxAudioGuidance = mockk(relaxed = true)

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

        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.current() } returns null

        mockkObject(MapboxAudioGuidance.Companion)
        every { MapboxAudioGuidance.getRegisteredInstance() } returns audioGuidance

        sut = MapboxNavigationViewApi(testStore)
    }

    @After
    fun cleanUp() {
        unmockkAll()
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
    @Suppress("MaxLineLength")
    fun `startRoutePreview should return an MissingDestinationInfo error if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        val error = sut.startRoutePreview().error

        assertEquals(NavigationViewApiErrorTypes.MissingDestinationInfo, error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should fail with MissingPreviewRoutesInfo error if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }

        val result = sut.startRoutePreview()

        assertEquals(NavigationViewApiErrorTypes.MissingPreviewRoutesInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                Point.fromLngLat(1.0, 2.0),
                Point.fromLngLat(2.0, 3.0),
                point,
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
    fun `startRoutePreview should fail with InvalidRoutesInfo error if preview routes are empty`() {
        val result = sut.startRoutePreview(emptyList())

        assertEquals(NavigationViewApiErrorTypes.InvalidRoutesInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startRoutePreview should fail with IncompleteRoutesInfo error if preview routes is missing waypoints data`() {
        val result = sut.startRoutePreview(listOf(navigationRoute()))

        assertEquals(NavigationViewApiErrorTypes.IncompleteRoutesInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startActiveGuidance should set preview routes as routes and set ActiveNavigation NavigationState`() {
        val routes = listOf<NavigationRoute>(mockk())
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(routes)
            )
        }

        val result = sut.startActiveGuidance()

        verify { testStore.dispatch(RoutesAction.SetRoutes(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
        assertTrue(result.isValue)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startActiveGuidance should fail with MissingDestinationInfo error if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk()))
            )
        }

        val result = sut.startActiveGuidance()

        assertEquals(NavigationViewApiErrorTypes.MissingDestinationInfo, result.error?.type)
    }

    @Test
    fun `startActiveGuidance should fail with MissingPreviewRoutesInfo error if preview routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                previewRoutes = RoutePreviewState.Ready(emptyList())
            )
        }

        val result = sut.startActiveGuidance()

        assertEquals(NavigationViewApiErrorTypes.MissingPreviewRoutesInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startActiveGuidance should set destination, preview routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                Point.fromLngLat(1.0, 2.0),
                Point.fromLngLat(2.0, 3.0),
                point,
            )
        )

        val result = sut.startActiveGuidance(routes)

        verify { testStore.dispatch(DestinationAction.SetDestination(Destination(point))) }
        verify { testStore.dispatch(RoutePreviewAction.Ready(routes)) }
        verify {
            testStore.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
        assertTrue(result.isValue)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startActiveGuidance should fail with InvalidRoutesInfo error if preview routes are empty`() {
        val result = sut.startActiveGuidance(emptyList())

        assertEquals(NavigationViewApiErrorTypes.InvalidRoutesInfo, result.error?.type)
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
    @Suppress("MaxLineLength")
    fun `startArrival should fail with MissingDestinationInfo error if destination has not been set`() {
        testStore.updateState {
            it.copy(
                destination = null,
                routes = listOf(mockk())
            )
        }

        val result = sut.startArrival()

        assertEquals(NavigationViewApiErrorTypes.MissingDestinationInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startArrival should fail with MissingRoutesInfo error if routes are empty`() {
        testStore.updateState {
            it.copy(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                routes = emptyList()
            )
        }

        val result = sut.startArrival()

        assertEquals(NavigationViewApiErrorTypes.MissingRoutesInfo, result.error?.type)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `startArrival should set destination, routes and set RoutePreview NavigationState `() {
        val point = Point.fromLngLat(11.0, 22.0)
        val routes = listOf(
            navigationRoute(
                Point.fromLngLat(1.0, 2.0),
                Point.fromLngLat(2.0, 3.0),
                point,
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
    @Suppress("MaxLineLength")
    fun `startArrival should fail with InvalidRoutesInfo error if preview routes are empty`() {
        val result = sut.startArrival(emptyList())

        assertEquals(NavigationViewApiErrorTypes.InvalidRoutesInfo, result.error?.type)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `isReplayEnabled should return true when replay trip session is enabled `() {
        val mapboxNavigation = mockk<MapboxNavigation>()
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        every { mapboxNavigation.isReplayEnabled() } returns false
        assertFalse(sut.isReplayEnabled())
        every { mapboxNavigation.isReplayEnabled() } returns true
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

    @Test
    fun `getCurrentVoiceInstructionsPlayer returns valid player instance`() {
        every { audioGuidance.getCurrentVoiceInstructionsPlayer() } returns null
        assertNull(sut.getCurrentVoiceInstructionsPlayer())

        val player: MapboxVoiceInstructionsPlayer = mockk()
        every { audioGuidance.getCurrentVoiceInstructionsPlayer() } returns player
        assertEquals(player, sut.getCurrentVoiceInstructionsPlayer())

        every { audioGuidance.getCurrentVoiceInstructionsPlayer() } returns null
        assertNull(sut.getCurrentVoiceInstructionsPlayer())
    }

    @Test
    fun `recenterCamera should dispatch store action`() {
        sut.recenterCamera()
        verify { testStore.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following)) }
    }

    private fun navigationRoute(vararg waypoints: Point): NavigationRoute {
        return mockk {
            every { routeOptions } returns mockk(relaxed = true) {
                every { coordinatesList() } returns waypoints.toList()
            }
        }
    }
}
