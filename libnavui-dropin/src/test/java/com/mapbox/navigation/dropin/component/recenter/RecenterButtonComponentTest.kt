package com.mapbox.navigation.dropin.component.recenter

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.camera.CameraState
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.camera.TargetCameraMode
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.testing.MainCoroutineRule
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
class RecenterButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val cameraStateFlow = MutableStateFlow(CameraState())
    private val mockRecenterButton: MapboxExtendableButton = mockk {
        every { setState(any()) } just Runs
        every { visibility = View.VISIBLE } just Runs
        every { visibility = View.GONE } just Runs
        every { setOnClickListener(any()) } just Runs
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true)
    private val navigationStateFlow = MutableStateFlow<NavigationState>(NavigationState.FreeDrive)

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var navigationStateViewModel: NavigationStateViewModel
    private lateinit var recenterButtonComponent: RecenterButtonComponent

    @Before
    fun setUp() {
        cameraViewModel = mockk {
            every { state } returns cameraStateFlow
        }

        navigationStateViewModel = mockk {
            every { state } returns navigationStateFlow
        }

        recenterButtonComponent = RecenterButtonComponent(
            cameraViewModel,
            navigationStateViewModel,
            mockRecenterButton
        )
    }

    @Test
    fun `when camera mode is not idle recenter is not visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            cameraStateFlow.value = CameraState(cameraMode = TargetCameraMode.Following)

            verify { mockRecenterButton.isVisible = false }
        }

    @Test
    fun `when camera mode is idle recenter is visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            cameraStateFlow.value = CameraState(cameraMode = TargetCameraMode.Following)

            verify { mockRecenterButton.isVisible = true }
        }

    @Test
    fun `when navigation state is route preview recenter is not visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            navigationStateFlow.value = NavigationState.RoutePreview

            verify { mockRecenterButton.isVisible = false }
        }
}
