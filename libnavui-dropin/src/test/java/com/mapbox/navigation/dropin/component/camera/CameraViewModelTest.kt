package com.mapbox.navigation.dropin.component.camera

import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `default state is idle`() {
        val cameraViewModel = CameraViewModel()

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toIdle updates camera mode`() = coroutineRule.runBlockingTest {
        val cameraViewModel = CameraViewModel()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        cameraViewModel.onAttached(mockMapboxNavigation)
        cameraViewModel.invoke(CameraAction.ToIdle)

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Idle, cameraState.cameraMode)
    }

    @Test
    fun `when action toOverview updates camera mode`() = coroutineRule.runBlockingTest {
        val cameraViewModel = CameraViewModel()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        cameraViewModel.onAttached(mockMapboxNavigation)
        cameraViewModel.invoke(CameraAction.ToOverview)

        val cameraState = cameraViewModel.state.value

        assertEquals(TargetCameraMode.Overview, cameraState.cameraMode)
    }

    @Test
    fun `when action toFollowing updates camera mode and zoomUpdatesAllowed`() =
        coroutineRule.runBlockingTest {
            val cameraViewModel = CameraViewModel()
            val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.invoke(CameraAction.ToFollowing)

            val cameraState = cameraViewModel.state.value

            assertEquals(TargetCameraMode.Following, cameraState.cameraMode)
        }

    @Test
    fun `when action UpdatePadding updates cameraPadding`() =
        coroutineRule.runBlockingTest {
            val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
            val cameraViewModel = CameraViewModel()
            val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
            cameraViewModel.onAttached(mockMapboxNavigation)

            cameraViewModel.invoke(CameraAction.UpdatePadding(padding))

            assertEquals(padding, cameraViewModel.state.value.cameraPadding)
        }
}
