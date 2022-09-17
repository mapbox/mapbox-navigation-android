package com.mapbox.navigation.dropin

import android.view.KeyEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.dropin.binder.map.MapClickBehavior
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelBehavior
import com.mapbox.navigation.dropin.component.maneuver.ManeuverBehavior
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalPreviewMapboxNavigationAPI
class NavigationViewListenerRegistryTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: NavigationViewListenerRegistry
    private lateinit var testStore: TestStore
    private lateinit var maneuverBehaviorFlow: MutableStateFlow<MapboxManeuverViewState>
    private lateinit var infoPanelBehavior: InfoPanelBehavior
    private lateinit var mapClickBehavior: MapClickBehavior
    private lateinit var testListener: NavigationViewListener
    private lateinit var slideOffsetFlow: MutableStateFlow<Float>

    @Before
    fun setUp() {
        testStore = TestStore()
        infoPanelBehavior = InfoPanelBehavior()
        mapClickBehavior = MapClickBehavior()
        maneuverBehaviorFlow = MutableStateFlow(MapboxManeuverViewState.COLLAPSED)
        slideOffsetFlow = MutableStateFlow(-1f)
        val mockManeuverBehavior = mockk<ManeuverBehavior> {
            every { maneuverBehavior } returns maneuverBehaviorFlow.asStateFlow()
        }
        testListener = spyk(object : NavigationViewListener() {})

        sut = NavigationViewListenerRegistry(
            testStore,
            mockManeuverBehavior,
            infoPanelBehavior,
            mapClickBehavior,
            coroutineRule.coroutineScope
        )
    }

    @Test
    fun onDestinationChanged() {
        sut.registerListener(testListener)

        val point = Point.fromLngLat(1.0, 2.0)
        val destination = Destination(Point.fromLngLat(1.0, 2.0))
        testStore.setState(State(destination = destination))

        verifyOrder {
            testListener.onDestinationChanged(null)
            testListener.onDestinationChanged(point)
        }
    }

    @Test
    fun onFreeDrive() {
        sut.registerListener(testListener)

        val navigationState = NavigationState.FreeDrive
        testStore.setState(State(navigation = navigationState))

        verify { testListener.onFreeDrive() }
    }

    @Test
    fun onDestinationPreview() {
        sut.registerListener(testListener)

        val navigationState = NavigationState.DestinationPreview
        testStore.setState(State(navigation = navigationState))

        verify { testListener.onDestinationPreview() }
    }

    @Test
    fun onRoutePreview() {
        sut.registerListener(testListener)

        val navigationState = NavigationState.RoutePreview
        testStore.setState(State(navigation = navigationState))

        verify { testListener.onRoutePreview() }
    }

    @Test
    fun onActiveNavigation() {
        sut.registerListener(testListener)

        val navigationState = NavigationState.ActiveNavigation
        testStore.setState(State(navigation = navigationState))

        verify { testListener.onActiveNavigation() }
    }

    @Test
    fun onArrival() {
        sut.registerListener(testListener)

        val navigationState = NavigationState.Arrival
        testStore.setState(State(navigation = navigationState))

        verify { testListener.onArrival() }
    }

    @Test
    fun onIdleCameraMode() {
        sut.registerListener(testListener)

        val cameraMode = TargetCameraMode.Idle
        val cameraState = testStore.state.value.camera.copy(cameraMode = cameraMode)
        testStore.setState(State(camera = cameraState))

        verify { testListener.onIdleCameraMode() }
    }

    @Test
    fun onFollowingCameraMode() {
        sut.registerListener(testListener)

        val cameraMode = TargetCameraMode.Following
        val cameraState = testStore.state.value.camera.copy(cameraMode = cameraMode)
        testStore.setState(State(camera = cameraState))

        verify { testListener.onFollowingCameraMode() }
    }

    @Test
    fun onOverviewCameraMode() {
        sut.registerListener(testListener)

        val cameraMode = TargetCameraMode.Overview
        val cameraState = testStore.state.value.camera.copy(cameraMode = cameraMode)
        testStore.setState(State(camera = cameraState))

        verify { testListener.onOverviewCameraMode() }
    }

    @Test
    fun onCameraPaddingChanged() {
        sut.registerListener(testListener)

        val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
        val cameraState = testStore.state.value.camera.copy(cameraPadding = padding)
        testStore.setState(State(camera = cameraState))

        verify { testListener.onCameraPaddingChanged(padding) }
    }

    @Test
    fun onAudioGuidanceStateChanged() {
        sut.registerListener(testListener)

        val isMuted = true
        val audioState = testStore.state.value.audio.copy(isMuted = isMuted)
        testStore.setState(State(audio = audioState))

        verify { testListener.onAudioGuidanceStateChanged(isMuted) }
    }

    @Test
    fun onRouteFetchedSuccessful_routesEmpty() {
        sut.registerListener(testListener)

        val routes = emptyList<NavigationRoute>()
        testStore.setState(State(previewRoutes = RoutePreviewState.Ready(routes)))

        verify {
            testListener.onRouteFetchSuccessful(routes)
        }
    }

    @Test
    fun onRouteFetchedSuccessful_routesNotEmpty() {
        sut.registerListener(testListener)

        val routes = listOf<NavigationRoute>(mockk())
        testStore.setState(State(previewRoutes = RoutePreviewState.Ready(routes)))

        verify {
            testListener.onRouteFetchSuccessful(routes)
        }
    }

    @Test
    fun onRouteFetchedFailed() {
        sut.registerListener(testListener)

        val reasons = listOf<RouterFailure>(mockk())
        val options = mockk<RouteOptions>()
        testStore.setState(State(previewRoutes = RoutePreviewState.Failed(reasons, options)))

        verify {
            testListener.onRouteFetchFailed(reasons, options)
        }
    }

    @Test
    fun onRouteFetchedCanceled() {
        sut.registerListener(testListener)

        val origin = mockk<RouterOrigin>()
        val options = mockk<RouteOptions>()
        testStore.setState(State(previewRoutes = RoutePreviewState.Canceled(options, origin)))

        verify {
            testListener.onRouteFetchCanceled(options, origin)
        }
    }

    @Test
    fun onRouteFetching() {
        sut.registerListener(testListener)

        val id = 1L
        testStore.setState(State(previewRoutes = RoutePreviewState.Fetching(id)))

        verify {
            testListener.onRouteFetching(id)
        }
    }

    @Test
    fun onInfoPanelHidden() {
        sut.registerListener(testListener)
        val newState = BottomSheetBehavior.STATE_HIDDEN

        infoPanelBehavior.updateBottomSheetState(newState)

        verify {
            testListener.onInfoPanelHidden()
        }
    }

    @Test
    fun onInfoPanelExpanded() {
        sut.registerListener(testListener)
        val newState = BottomSheetBehavior.STATE_EXPANDED

        infoPanelBehavior.updateBottomSheetState(newState)

        verify {
            testListener.onInfoPanelExpanded()
        }
    }

    @Test
    fun onInfoPanelCollapsed() {
        sut.registerListener(testListener)
        val newState = BottomSheetBehavior.STATE_COLLAPSED

        infoPanelBehavior.updateBottomSheetState(newState)

        verify {
            testListener.onInfoPanelCollapsed()
        }
    }

    @Test
    fun onInfoPanelDragging() {
        sut.registerListener(testListener)
        val newState = BottomSheetBehavior.STATE_DRAGGING

        infoPanelBehavior.updateBottomSheetState(newState)

        verify {
            testListener.onInfoPanelDragging()
        }
    }

    @Test
    fun onInfoPanelSettling() {
        sut.registerListener(testListener)
        val newState = BottomSheetBehavior.STATE_SETTLING

        infoPanelBehavior.updateBottomSheetState(newState)

        verify {
            testListener.onInfoPanelSettling()
        }
    }

    @Test
    fun `onRouteFetching should NOT notify listener if requestId is 0`() {
        sut.registerListener(testListener)

        val id = 0L
        testStore.setState(State(previewRoutes = RoutePreviewState.Fetching(id)))

        verify(exactly = 0) { testListener.onRouteFetching(id) }
    }

    @Test
    fun `onKey should notify listeners only when BACK button is pressed`() {
        sut.registerListener(testListener)

        val keyEvent = mockk<KeyEvent> { every { action } returns KeyEvent.ACTION_UP }
        sut.onKey(mockk(), KeyEvent.KEYCODE_BACK, keyEvent)

        verify { testListener.onBackPressed() }
    }

    @Test
    fun `onKey should NOT notify remaining listeners once BACK button pressed event is consumed`() {
        val listener1 = mockk<NavigationViewListener>(relaxed = true) {
            every { onBackPressed() } returns true
        }
        val listener2 = mockk<NavigationViewListener>(relaxed = true)
        sut.registerListener(listener1)
        sut.registerListener(listener2)

        val keyEvent = mockk<KeyEvent> { every { action } returns KeyEvent.ACTION_UP }
        val result = sut.onKey(mockk(), KeyEvent.KEYCODE_BACK, keyEvent)

        assertTrue(result)
        verify(exactly = 1) { listener1.onBackPressed() }
        verify(exactly = 0) { listener2.onBackPressed() }
    }

    @Test
    fun `onKey should NOT notify listeners when other buttons are pressed`() {
        sut.registerListener(testListener)

        val keyEvent = mockk<KeyEvent> { every { action } returns KeyEvent.ACTION_UP }
        sut.onKey(mockk(), KeyEvent.KEYCODE_1, keyEvent)
        sut.onKey(mockk(), KeyEvent.KEYCODE_2, keyEvent)
        sut.onKey(mockk(), KeyEvent.KEYCODE_3, keyEvent)

        verify(exactly = 0) { testListener.onBackPressed() }
    }

    @Test
    fun onManeuverExpanded() {
        sut.registerListener(testListener)
        val newState = MapboxManeuverViewState.EXPANDED

        maneuverBehaviorFlow.value = newState

        verify {
            testListener.onManeuverExpanded()
        }
    }

    @Test
    fun onManeuverCollapsed() {
        sut.registerListener(testListener)
        val expanded = MapboxManeuverViewState.EXPANDED
        maneuverBehaviorFlow.value = expanded
        val collapsed = MapboxManeuverViewState.COLLAPSED
        maneuverBehaviorFlow.value = collapsed

        verify {
            testListener.onManeuverCollapsed()
        }
    }

    @Test
    fun onMapClicked() {
        sut.registerListener(testListener)
        val point = mockk<Point>()
        mapClickBehavior.onMapClicked(point)

        verify { testListener.onMapClicked(point) }
    }

    @Test
    fun onInfoPanelSlide() {
        sut.registerListener(testListener)

        infoPanelBehavior.updateSlideOffset(0.6f)

        verify {
            testListener.onInfoPanelSlide(0.6f)
        }
    }
}
