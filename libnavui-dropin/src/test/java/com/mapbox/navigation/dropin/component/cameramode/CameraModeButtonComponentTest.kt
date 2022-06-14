package com.mapbox.navigation.dropin.component.cameramode

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraModeButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mockCameraModeButton: MapboxCameraModeButton = mockk {
        every { setState(any()) } just Runs
        every { visibility = View.VISIBLE } just Runs
        every { visibility = View.GONE } just Runs
        every { setOnClickListener(any()) } just Runs
        every { updateStyle(any()) } just Runs
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true)

    private lateinit var testStore: TestStore

    private lateinit var cameraModeButtonComponent: CameraModeButtonComponent

    @Before
    fun setUp() {
        testStore = spyk(TestStore())

        cameraModeButtonComponent = CameraModeButtonComponent(
            testStore,
            mockCameraModeButton,
            R.style.MapboxStyleCameraModeButton
        )
    }

    @Test
    fun `when camera mode is following button icon is overview`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                State(
                    camera = mockk {
                        every { cameraMode } returns TargetCameraMode.Following
                    },
                )
            )

            verify { mockCameraModeButton.setState(NavigationCameraState.FOLLOWING) }
        }

    @Test
    fun `when camera mode is overview button icon is following`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                State(
                    camera = mockk {
                        every { cameraMode } returns TargetCameraMode.Overview
                    },
                )
            )

            verify { mockCameraModeButton.setState(NavigationCameraState.OVERVIEW) }
        }

    @Test
    fun `when navigation state is route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(State(navigation = NavigationState.RoutePreview))

            verify { mockCameraModeButton.isVisible = false }
        }

    @Test
    fun `when navigation state is not route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            cameraModeButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(State(navigation = NavigationState.ActiveNavigation))

            verify { mockCameraModeButton.isVisible = true }
        }
}
