package com.mapbox.navigation.dropin.camera

import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecenterButtonComponentContractImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var sut: RecenterButtonComponentContractImpl

    @Before
    fun setUp() {
        store = spyk(TestStore())
        sut = RecenterButtonComponentContractImpl(coroutineRule.coroutineScope, store)
    }

    @Test
    fun `isVisible - should return TRUE when camera is Idle`() =
        coroutineRule.runBlockingTest {
            store.updateState {
                it.copy(
                    camera = it.camera.copy(cameraMode = TargetCameraMode.Idle),
                    navigation = NavigationState.FreeDrive
                )
            }
            coroutineRule.testDispatcher.advanceUntilIdle()
            assertTrue(
                "expected TRUE when camera is Idle",
                sut.isVisible.value
            )
        }

    @Test
    fun `isVisible - should return FALSE when camera not Idle`() =
        coroutineRule.runBlockingTest {
            store.updateState {
                it.copy(
                    camera = it.camera.copy(cameraMode = TargetCameraMode.Following),
                    navigation = NavigationState.FreeDrive
                )
            }
            coroutineRule.testDispatcher.advanceUntilIdle()
            assertFalse("expected FALSE when camera not Idle", sut.isVisible.value)
        }

    @Test
    fun `onClick should dispatch SetCameraMode action`() {
        sut.onClick(mockk())

        verify { store.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following)) }
    }
}
