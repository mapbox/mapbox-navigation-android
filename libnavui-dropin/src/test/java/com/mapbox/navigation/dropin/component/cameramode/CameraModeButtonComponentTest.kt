package com.mapbox.navigation.dropin.component.cameramode

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraState
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.camera.TargetCameraMode
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraModeButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val cameraStateFlow = MutableStateFlow(CameraState())
    private val mockCameraModeButton: MapboxCameraModeButton = mockk {
        every { setState(any()) } just Runs
        every { visibility = View.VISIBLE } just Runs
        every { visibility = View.GONE } just Runs
        every { setOnClickListener(any()) } just Runs
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true)
    private val navigationStateFlow = MutableStateFlow<NavigationState>(NavigationState.FreeDrive)

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var navigationStateViewModel: NavigationStateViewModel

    private lateinit var cameraModeButtonComponent: CameraModeButtonComponent

    @Before
    fun setUp() {
        cameraViewModel = mockk {
            every { state } returns cameraStateFlow
        }

        navigationStateViewModel = mockk {
            every { state } returns navigationStateFlow
        }

        cameraModeButtonComponent = CameraModeButtonComponent(
            cameraViewModel,
            navigationStateViewModel,
            mockCameraModeButton
        )
    }

    @Test
    fun `when camera mode is following button icon is overview`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            cameraStateFlow.value = CameraState(cameraMode = TargetCameraMode.Following)

            verify { mockCameraModeButton.setState(NavigationCameraState.FOLLOWING) }
        }

    @Test
    fun `when camera mode is overview button icon is following`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            cameraStateFlow.value = CameraState(cameraMode = TargetCameraMode.Overview)

            verify { mockCameraModeButton.setState(NavigationCameraState.OVERVIEW) }
        }

    @Test
    fun `when navigation state is route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            navigationStateFlow.value = NavigationState.RoutePreview

            verify { mockCameraModeButton.isVisible = false }
        }

    @Test
    fun `when navigation state is not route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            navigationStateFlow.value = NavigationState.ActiveNavigation

            verify { mockCameraModeButton.isVisible = true }
        }
}
