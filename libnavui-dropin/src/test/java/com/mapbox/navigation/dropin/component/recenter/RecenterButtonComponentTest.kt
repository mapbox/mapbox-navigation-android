package com.mapbox.navigation.dropin.component.recenter

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class RecenterButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mockRecenterButton: MapboxExtendableButton = mockk {
        every { setState(any()) } just Runs
        every { visibility = View.VISIBLE } just Runs
        every { visibility = View.GONE } just Runs
        every { setOnClickListener(any()) } just Runs
        every { updateStyle(any()) } just Runs
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true)

    private lateinit var recenterButtonComponent: RecenterButtonComponent
    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        testStore = TestStore()

        recenterButtonComponent = RecenterButtonComponent(
            testStore,
            R.style.DropInStyleRecenterButton,
            mockRecenterButton
        )
    }

    @Test
    fun `when camera mode is not idle recenter is not visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                State(camera = mockk { every { cameraMode } returns TargetCameraMode.Following })
            )

            verify { mockRecenterButton.isVisible = false }
        }

    @Test
    fun `when camera mode is idle recenter is visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                State(camera = mockk { every { cameraMode } returns TargetCameraMode.Following })
            )

            verify { mockRecenterButton.isVisible = true }
        }

    @Test
    fun `when navigation state is route preview recenter is not visible`() =
        coroutineRule.runBlockingTest {
            recenterButtonComponent.onAttached(mockMapboxNavigation)

            testStore.setState(State(navigation = NavigationState.RoutePreview))

            verify { mockRecenterButton.isVisible = false }
        }
}
