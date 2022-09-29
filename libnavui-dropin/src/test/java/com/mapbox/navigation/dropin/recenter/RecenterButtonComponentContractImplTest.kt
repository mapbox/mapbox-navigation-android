package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class RecenterButtonComponentContractImplTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var sut: RecenterButtonComponentContractImpl

    @Before
    fun setUp() {
        store = spyk(TestStore())
        sut = RecenterButtonComponentContractImpl(coroutineRule.coroutineScope, store)
    }

    @Test
    fun `isVisible - should return TRUE when camera is Idle and not in RoutePreview`() =
        coroutineRule.runBlockingTest {
            store.updateState {
                it.copy(
                    camera = it.camera.copy(cameraMode = TargetCameraMode.Idle),
                    navigation = NavigationState.FreeDrive
                )
            }
            yield() // yielding to allow isVisible StateFlow run its logic
            assertTrue(
                "expected TRUE when camera is Idle and not in RoutePreview",
                sut.isVisible.value
            )
        }

    @Test
    fun `isVisible - should return FALSE when camera not Idle or in RoutePreview`() =
        coroutineRule.runBlockingTest {
            store.updateState {
                it.copy(
                    camera = it.camera.copy(cameraMode = TargetCameraMode.Following),
                    navigation = NavigationState.FreeDrive
                )
            }
            yield() // yielding to allow isVisible StateFlow run its logic
            assertFalse("expected FALSE when camera not Idle", sut.isVisible.value)

            store.updateState {
                it.copy(
                    camera = it.camera.copy(cameraMode = TargetCameraMode.Idle),
                    navigation = NavigationState.RoutePreview
                )
            }
            yield() // yielding to allow isVisible StateFlow run its logic
            assertFalse("expected FALSE when NavigationState not RoutePreview", sut.isVisible.value)
        }

    @Test
    fun `onClick should dispatch SetCameraMode action`() {
        sut.onClick(mockk())

        verify { store.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following)) }
    }
}
