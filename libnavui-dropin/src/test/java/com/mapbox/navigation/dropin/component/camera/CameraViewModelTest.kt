package com.mapbox.navigation.dropin.component.camera

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `default state is idle`() {
        val cameraViewModel = CameraViewModel()

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toIdle updates camera mode`() = coroutineRule.runBlockingTest {
        val cameraViewModel = CameraViewModel()
        cameraViewModel.onAttached(mockMapboxNavigation())
        cameraViewModel.invoke(CameraAction.ToIdle)

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toOverview updates camera mode`() = coroutineRule.runBlockingTest {
        val cameraViewModel = CameraViewModel()
        cameraViewModel.onAttached(mockMapboxNavigation())
        cameraViewModel.invoke(CameraAction.ToOverview)

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Overview, cameraState.cameraMode)
    }

    @Test
    fun `when action toFollowing updates camera mode and zoomUpdatesAllowed`() =
        coroutineRule.runBlockingTest {
            val cameraViewModel = CameraViewModel()
            cameraViewModel.onAttached(mockMapboxNavigation())
            cameraViewModel.invoke(CameraAction.ToFollowing)

            val cameraState = cameraViewModel.state.value

            assertEquals(TargetCameraMode.Following, cameraState.cameraMode)
        }

    @Test
    fun `when action UpdatePadding updates cameraPadding`() =
        coroutineRule.runBlockingTest {
            val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
            val cameraViewModel = CameraViewModel()
            cameraViewModel.onAttached(mockMapboxNavigation())

            cameraViewModel.invoke(CameraAction.UpdatePadding(padding))

            assertEquals(padding, cameraViewModel.state.value.cameraPadding)
        }

    @Test
    fun `saveCameraState should save map camera state after detached`() {
        val cameraState = com.mapbox.maps.CameraState(
            Point.fromLngLat(11.0, 22.0),
            EdgeInsets(1.0, 2.0, 3.0, 4.0),
            30.0,
            40.0,
            50.0
        )
        val mockMapboxNavigation = mockMapboxNavigation()
        val cameraViewModel = CameraViewModel()

        cameraViewModel.onAttached(mockMapboxNavigation)
        cameraViewModel.onDetached(mockMapboxNavigation)
        cameraViewModel.invoke(CameraAction.SaveMapState(cameraState))

        assertEquals(cameraState, cameraViewModel.state.value.mapCameraState)
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}
