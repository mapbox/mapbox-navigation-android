package com.mapbox.navigation.dropin.component.cameramode

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponentContract
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraModeButtonComponentContractImplTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var mockMapboxNavigation: MapboxNavigation
    private lateinit var testStore: TestStore

    private lateinit var sut: CameraModeButtonComponentContract

    @Before
    fun setUp() {
        testStore = spyk(TestStore())
        mockMapboxNavigation = mockk(relaxed = true)
        sut = CameraModeButtonComponentContractImpl(
            coroutineRule.coroutineScope,
            testStore,
        )
    }

    @Test
    fun `buttonState should map TargetCameraMode to NavigationCameraState`() = runBlockingTest {
        testStore.updateState {
            it.copy(camera = it.camera.copy(cameraMode = TargetCameraMode.Following))
        }
        val buttonStates = mutableListOf<NavigationCameraState>()
        val job = launch {
            sut.buttonState.take(2).toList(buttonStates)
            yield()
        }
        testStore.updateState {
            it.copy(camera = it.camera.copy(cameraMode = TargetCameraMode.Overview))
        }

        job.join()
        assertEquals(
            listOf(NavigationCameraState.FOLLOWING, NavigationCameraState.OVERVIEW),
            buttonStates
        )
    }

    @Test
    fun `buttonState when TargetCameraMode is Idle should return saved state`() = runBlockingTest {
        testStore.updateState {
            it.copy(
                camera = it.camera.copy(
                    cameraMode = TargetCameraMode.Idle,
                    savedCameraMode = TargetCameraMode.Following,
                )
            )
        }

        val navCamState = sut.buttonState.take(1).toList().first()

        assertEquals(NavigationCameraState.FOLLOWING, navCamState)
    }

    @Test
    fun `isVisible use NavigationState to determine visibility`() = runBlockingTest {
        testStore.updateState {
            it.copy(navigation = NavigationState.RoutePreview)
        }
        val visibility = mutableListOf<Boolean>()
        val job = launch {
            sut.isVisible.take(2).toList(visibility)
            yield()
        }
        testStore.updateState {
            it.copy(navigation = NavigationState.ActiveNavigation)
        }

        job.join()
        assertEquals(listOf(false, true), visibility)
    }

    @Test
    fun `onClick should request Overview mode current is Following`() = runBlockingTest {
        testStore.updateState {
            it.copy(
                camera = it.camera.copy(
                    cameraMode = TargetCameraMode.Following
                )
            )
        }

        sut.onClick(mockk())

        verify { testStore.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Overview)) }
    }

    @Test
    fun `onClick should request Following mode current is Overview`() = runBlockingTest {
        testStore.updateState {
            it.copy(
                camera = it.camera.copy(
                    cameraMode = TargetCameraMode.Overview
                )
            )
        }

        sut.onClick(mockk())

        verify { testStore.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following)) }
    }

    @Test
    fun `onClick should use savedCameraMode mode value when current is Idle`() = runBlockingTest {
        testStore.updateState {
            it.copy(
                camera = it.camera.copy(
                    cameraMode = TargetCameraMode.Idle,
                    savedCameraMode = TargetCameraMode.Overview,
                )
            )
        }

        sut.onClick(mockk())

        verify { testStore.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following)) }
    }
}
