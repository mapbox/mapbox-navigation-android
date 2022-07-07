package com.mapbox.navigation.ui.app.internal.tripsession

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationPermissionComponent
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@ExperimentalPreviewMapboxNavigationAPI
class LocationPermissionComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testLauncher: ActivityResultLauncher<Any>
    private lateinit var resultContractSlot: CapturingSlot<ActivityResultContract<Any, Any>>
    private lateinit var callbackSlot: CapturingSlot<ActivityResultCallback<Any>>
    private lateinit var testLifecycle: TestLifecycleOwner
    private lateinit var componentActivity: ComponentActivity
    private lateinit var testStore: TestStore
    private lateinit var sut: LocationPermissionComponent

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
        mockkStatic(Lifecycle::class)
        testLauncher = mockk(relaxed = true)
        resultContractSlot = slot()
        callbackSlot = slot()
        testStore = spyk(TestStore())
        testLifecycle = TestLifecycleOwner()
        componentActivity = mockk(relaxed = true) {
            every { lifecycle } returns testLifecycle.lifecycle
            every {
                registerForActivityResult(
                    capture(resultContractSlot),
                    capture(callbackSlot)
                )
            } answers {
                testLauncher
            }
        }

        sut = LocationPermissionComponent(WeakReference(componentActivity), testStore)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached when activity starts, should check permissions and notify when permissions are granted`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            sut.onAttached(mockMapboxNavigation())

            testLifecycle.moveToState(Lifecycle.State.STARTED)

            verify {
                testStore.dispatch(TripSessionStarterAction.OnLocationPermission(true))
            }
        }

    @Test
    fun `onAttached when activity starts, should check permissions and request permissions if not granted`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
            sut.onAttached(mockMapboxNavigation())

            testLifecycle.moveToState(Lifecycle.State.STARTED)

            verify { testLauncher.launch(any()) }
        }

    @Test
    fun `on ActivityResultLauncher result, should notify when permissions are granted`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
            sut.onAttached(mockMapboxNavigation())

            testLifecycle.moveToState(Lifecycle.State.STARTED)
            callbackSlot.captured.onActivityResult(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to true,
                    Manifest.permission.ACCESS_COARSE_LOCATION to true,
                )
            )

            verify {
                testStore.dispatch(TripSessionStarterAction.OnLocationPermission(true))
            }
        }

    @Test
    fun `on ActivityResultLauncher result, should notify when permissions are denied`() =
        coroutineRule.runBlockingTest {
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
            sut.onAttached(mockMapboxNavigation())

            testLifecycle.moveToState(Lifecycle.State.STARTED)
            callbackSlot.captured.onActivityResult(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to false,
                    Manifest.permission.ACCESS_COARSE_LOCATION to false,
                )
            )

            verify {
                testStore.dispatch(TripSessionStarterAction.OnLocationPermission(false))
            }
        }

    @Test
    fun `onDetached should unregister the ActivityResultLauncher`() {
        sut.onAttached(mockMapboxNavigation())
        sut.onDetached(mockMapboxNavigation())

        verify { testLauncher.unregister() }
    }

    @Test
    fun `should invoke LocationPermissionResult when permissions are accepted from background`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                State(
                    tripSession = mockk {
                        every { isLocationPermissionGranted } returns false
                    }
                )
            )
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

            sut.onAttached(mockMapboxNavigation())
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            testLifecycle.lifecycleRegistry.currentState = Lifecycle.State.STARTED

            verify {
                testStore.dispatch(
                    TripSessionStarterAction.OnLocationPermission(true)
                )
            }
        }

    private fun mockMapboxNavigation(): MapboxNavigation = mockk(relaxed = true)

    private class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry

        fun moveToState(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}
