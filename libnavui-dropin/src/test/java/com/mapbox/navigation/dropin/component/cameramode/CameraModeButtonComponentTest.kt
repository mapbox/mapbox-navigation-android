package com.mapbox.navigation.dropin.component.cameramode

import android.content.Context
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CameraModeButtonComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var cameraModeButton: MapboxCameraModeButton
    private lateinit var mockMapboxNavigation: MapboxNavigation
    private lateinit var testStore: TestStore

    private lateinit var sut: CameraModeButtonComponent

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        cameraModeButton = spyk(MapboxCameraModeButton(context, null))
        testStore = spyk(TestStore())
        mockMapboxNavigation = mockk(relaxed = true)
        sut = CameraModeButtonComponent(
            testStore,
            cameraModeButton,
            R.style.MapboxStyleCameraModeButton
        )
    }

    @Test
    fun `when camera mode is following button icon is overview`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockMapboxNavigation)

            val cameraState = testStore.state.value.camera.copy(
                cameraMode = TargetCameraMode.Following
            )
            testStore.setState(State(camera = cameraState))

            verify { cameraModeButton.setState(NavigationCameraState.FOLLOWING) }
        }

    @Test
    fun `when camera mode is overview button icon is following`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockMapboxNavigation)

            val cameraState = testStore.state.value.camera.copy(
                cameraMode = TargetCameraMode.Overview
            )
            testStore.setState(State(camera = cameraState))

            verify { cameraModeButton.setState(NavigationCameraState.OVERVIEW) }
        }

    @Test
    fun `when navigation state is route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockMapboxNavigation)

            testStore.setState(State(navigation = NavigationState.RoutePreview))

            assertFalse(cameraModeButton.isVisible)
        }

    @Test
    fun `when navigation state is not route preview camera mode button is not visible`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockMapboxNavigation)

            testStore.setState(State(navigation = NavigationState.ActiveNavigation))

            assertTrue(cameraModeButton.isVisible)
        }

    @Test
    fun `when in Following mode onClick cameraModeButton should switch to Overview`() =
        coroutineRule.runBlockingTest {
            val cameraState = testStore.state.value.camera.copy(
                cameraMode = TargetCameraMode.Following
            )
            testStore.setState(State(camera = cameraState))
            sut.onAttached(mockMapboxNavigation)

            cameraModeButton.performClick()

            verify { testStore.dispatch(SetCameraMode(TargetCameraMode.Overview)) }
        }

    @Test
    fun `when in Overview mode onClick cameraModeButton should switch to Following`() =
        coroutineRule.runBlockingTest {
            val cameraState = testStore.state.value.camera.copy(
                cameraMode = TargetCameraMode.Overview
            )
            testStore.setState(State(camera = cameraState))
            sut.onAttached(mockMapboxNavigation)

            cameraModeButton.performClick()

            verify { testStore.dispatch(SetCameraMode(TargetCameraMode.Following)) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `when in Idle mode onClick cameraModeButton should use savedCameraMode to determine target camera mode`() =
        coroutineRule.runBlockingTest {
            val cameraState = testStore.state.value.camera.copy(
                cameraMode = TargetCameraMode.Idle,
                savedCameraMode = TargetCameraMode.Following
            )
            testStore.setState(State(camera = cameraState))
            sut.onAttached(mockMapboxNavigation)

            cameraModeButton.performClick()

            verify { testStore.dispatch(SetCameraMode(TargetCameraMode.Overview)) }
        }

    @Test
    fun `onDetached should remove OnClickListener`() = coroutineRule.runBlockingTest {
        sut.onAttached(mockMapboxNavigation)
        sut.onDetached(mockMapboxNavigation)

        verify { cameraModeButton.setOnClickListener(null) }
    }
}
