package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.CameraState
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testStore: TestStore

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        testStore = spyk(TestStore())
    }

    @After
    fun teardown() {
        unmockkObject(MapboxNavigationApp)
    }

    @Test
    fun `when action toIdle updates camera mode`() = coroutineRule.runBlockingTest {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(SetCameraMode(TargetCameraMode.Idle))

        val cameraState = testStore.state.value.camera
        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toIdle should copy currentCamera mode value to savedCameraMode`() =
        coroutineRule.runBlockingTest {
            val initialState = State(camera = CameraState(cameraMode = TargetCameraMode.Following))
            testStore.setState(initialState)

            val sut = CameraStateController(testStore)
            sut.onAttached(mockMapboxNavigation())
            testStore.dispatch(SetCameraMode(TargetCameraMode.Idle))

            val cameraState = testStore.state.value.camera
            assertEquals(initialState.camera.cameraMode, cameraState.savedCameraMode)
        }

    @Test
    fun `when action toOverview updates camera mode`() = coroutineRule.runBlockingTest {
        val sut = CameraStateController(testStore)
        sut.onAttached(mockMapboxNavigation())

        testStore.dispatch(SetCameraMode(TargetCameraMode.Overview))

        val cameraState = testStore.state.value.camera
        assertEquals(TargetCameraMode.Overview, cameraState.cameraMode)
    }

    @Test
    fun `when action toFollowing updates camera mode and zoomUpdatesAllowed`() =
        coroutineRule.runBlockingTest {
            val sut = CameraStateController(testStore)
            sut.onAttached(mockMapboxNavigation())

            testStore.dispatch(SetCameraMode(TargetCameraMode.Following))

            val cameraState = testStore.state.value.camera
            assertEquals(TargetCameraMode.Following, cameraState.cameraMode)
        }

    @Test
    fun `when action UpdatePadding updates cameraPadding`() =
        coroutineRule.runBlockingTest {
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

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
