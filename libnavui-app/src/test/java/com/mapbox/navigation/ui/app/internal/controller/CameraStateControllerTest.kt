package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraStateControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val testStore = TestStore()

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun teardown() {
        unmockkObject(MapboxNavigationApp)
    }

    @Test
    fun `when action toIdle updates camera mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(SetCameraMode(TargetCameraMode.Idle))

        val cameraState = testStore.state.value.camera
        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toIdle should copy currentCamera mode value to savedCameraMode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        val initialCameraMode = TargetCameraMode.Following
        testStore.dispatch(SetCameraMode(initialCameraMode))
        testStore.dispatch(SetCameraMode(TargetCameraMode.Idle))

        val cameraState = testStore.state.value.camera
        assertEquals(initialCameraMode, cameraState.savedCameraMode)
    }

    @Test
    fun `when action toOverview updates camera mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(SetCameraMode(TargetCameraMode.Overview))

        val cameraState = testStore.state.value.camera
        assertEquals(TargetCameraMode.Overview, cameraState.cameraMode)
    }

    @Test
    fun `when action toFollowing updates camera mode and zoomUpdatesAllowed`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))

        val cameraState = testStore.state.value.camera
        assertEquals(TargetCameraMode.Following, cameraState.cameraMode)
    }

    @Test
    fun `when action UpdatePadding updates cameraPadding`() {
        val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(CameraAction.UpdatePadding(padding))

        val cameraState = testStore.state.value.camera
        assertEquals(padding, cameraState.cameraPadding)
    }

    @Test
    fun `on SaveMapState action should save map camera state in the store`() {
        val cameraState = com.mapbox.maps.CameraState(
            Point.fromLngLat(11.0, 22.0),
            EdgeInsets(1.0, 2.0, 3.0, 4.0),
            30.0,
            40.0,
            50.0
        )
        val mockMapboxNavigation = mockMapboxNavigation()
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation)

        testStore.dispatch(CameraAction.SaveMapState(cameraState))

        assertEquals(cameraState, testStore.state.value.camera.mapCameraState)
    }

    @Test
    fun `camera is unchanged in route preview mode without preview routes`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.DestinationPreview) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))
        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview, previewRoutes = RoutePreviewState.Empty,
            )
        }

        assertEquals(TargetCameraMode.Following, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is set to overview in route preview mode with preview routes`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.DestinationPreview) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))
        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk())),
            )
        }

        assertEquals(TargetCameraMode.Overview, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is restored to overview in route preview mode with new preview routes`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk())),
            )
        }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))
        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview,
                previewRoutes = RoutePreviewState.Ready(listOf(mockk())),
            )
        }

        assertEquals(TargetCameraMode.Overview, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is unchanged in route preview mode with the same preview routes`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())
        val route = mockk<NavigationRoute>()

        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview,
                previewRoutes = RoutePreviewState.Ready(listOf(route)),
            )
        }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))
        testStore.updateState { state ->
            state.copy(
                navigation = NavigationState.RoutePreview,
                previewRoutes = RoutePreviewState.Ready(listOf(route)),
            )
        }

        assertEquals(TargetCameraMode.Following, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is set to overview in free drive mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.DestinationPreview) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Following))
        testStore.updateState { it.copy(navigation = NavigationState.FreeDrive) }

        assertEquals(TargetCameraMode.Overview, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is set to following in active navigation mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.RoutePreview) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Overview))
        testStore.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }

        assertEquals(TargetCameraMode.Following, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is set to following in arrival mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.ActiveNavigation) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Overview))
        testStore.updateState { it.copy(navigation = NavigationState.Arrival) }

        assertEquals(TargetCameraMode.Following, testStore.state.value.camera.cameraMode)
    }

    @Test
    fun `camera is set to idle in destination preview mode`() {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.updateState { it.copy(navigation = NavigationState.FreeDrive) }
        testStore.dispatch(SetCameraMode(TargetCameraMode.Overview))
        testStore.updateState { it.copy(navigation = NavigationState.DestinationPreview) }

        assertEquals(TargetCameraMode.Idle, testStore.state.value.camera.cameraMode)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
