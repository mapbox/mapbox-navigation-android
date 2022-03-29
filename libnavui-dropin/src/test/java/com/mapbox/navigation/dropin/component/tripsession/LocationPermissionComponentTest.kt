package com.mapbox.navigation.dropin.component.tripsession

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
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
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

    private val testLauncher = mockk<ActivityResultLauncher<Any>>(relaxed = true)
    private val resultContractSlot = slot<ActivityResultContract<Any, Any>>()
    private val callbackSlot = slot<ActivityResultCallback<Any>>()
    private val testLifecycle = TestLifecycleOwner()
    private var componentActivity: ComponentActivity = mockk(relaxed = true) {
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
    private val componentActivityRef = WeakReference(componentActivity)
    private var testStore: TestStore = spyk(TestStore())

    @Before
    fun setup() {
        mockkStatic(PermissionsManager::class)
        mockkStatic(Lifecycle::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached will notify permissions granted when granted`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true

        locationPermissionComponent.onAttached(mockMapboxNavigation())

        verify {
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(true)
            )
        }
    }

    @Test
    fun `onAttached will not notify permissions granted when not granted`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        locationPermissionComponent.onAttached(mockMapboxNavigation())

        verify(exactly = 0) {
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(false)
            )
        }
    }

    @Test
    fun `onAttached will request permissions when not granted`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        locationPermissionComponent.onAttached(mockMapboxNavigation())

        verify { testLauncher.launch(any()) }
    }

    @Test
    fun `onAttached grant location permissions if request succeeds`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        locationPermissionComponent.onAttached(mockMapboxNavigation())
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to true,
        )
        callbackSlot.captured.onActivityResult(permissions)

        verify {
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(true)
            )
        }
    }

    @Test
    fun `onAttached not grant location permissions if request is denied`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        locationPermissionComponent.onAttached(mockMapboxNavigation())
        val permissions = mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false,
        )
        callbackSlot.captured.onActivityResult(permissions)

        verify {
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(false)
            )
        }
    }

    @Test
    fun `onDetached will unregister from the launcher`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivityRef, testStore
        )

        locationPermissionComponent.onAttached(mockMapboxNavigation())
        locationPermissionComponent.onDetached(mockMapboxNavigation())

        verify { testLauncher.unregister() }
    }

    @Test
    fun `should invoke LocationPermissionResult when permissions are accepted from background`() =
        coroutineRule.runBlockingTest {
            val locationPermissionComponent = LocationPermissionComponent(
                componentActivityRef, testStore
            )
            testStore.setState(
                State(
                    tripSession = TripSessionStarterState(isLocationPermissionGranted = false)
                )
            )
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

            locationPermissionComponent.onAttached(mockMapboxNavigation())
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
    }
}
