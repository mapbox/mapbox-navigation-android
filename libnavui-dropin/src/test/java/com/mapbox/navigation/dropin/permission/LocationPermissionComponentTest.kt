package com.mapbox.navigation.dropin.permission

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationPermissionComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val testLauncher = mockk<ActivityResultLauncher<Any>>(relaxed = true)
    private val resultContractSlot = slot<ActivityResultContract<Any, Any>>()
    private val callbackSlot = slot<ActivityResultCallback<Any>>()
    private val testLifecycle = TestLifecycleOwner()
    private val componentActivity = mockk<ComponentActivity>(relaxed = true) {
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
    private val testStore = spyk(TestStore())

    @Before
    fun setup() {
        // todo this will have to be changed to `mockkStatic(PermissionsManager::class)`
        // when upgrading to Common SDK v23.2.0
        mockkObject(PermissionsManager)
        mockkStatic(Lifecycle::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached will notify permissions granted when granted`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivity,
            testStore
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
            componentActivity,
            testStore
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
            componentActivity,
            testStore
        )
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

        locationPermissionComponent.onAttached(mockMapboxNavigation())

        verify { testLauncher.launch(any()) }
    }

    @Test
    fun `onAttached grant location permissions if request succeeds`() {
        val locationPermissionComponent = LocationPermissionComponent(
            componentActivity,
            testStore
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
            componentActivity,
            testStore
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
            componentActivity,
            testStore
        )

        locationPermissionComponent.onAttached(mockMapboxNavigation())
        locationPermissionComponent.onDetached(mockMapboxNavigation())

        verify { testLauncher.unregister() }
    }

    @Test
    fun `should invoke LocationPermissionResult when permissions are accepted from background`() =
        coroutineRule.runBlockingTest {
            val locationPermissionComponent = LocationPermissionComponent(
                componentActivity,
                testStore
            )
            testStore.setState(
                State(
                    tripSession = mockk {
                        every { isLocationPermissionGranted } returns false
                    }
                )
            )
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false

            locationPermissionComponent.onAttached(mockMapboxNavigation())
            every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
            testLifecycle.lifecycleRegistry.currentState = Lifecycle.State.RESUMED

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
