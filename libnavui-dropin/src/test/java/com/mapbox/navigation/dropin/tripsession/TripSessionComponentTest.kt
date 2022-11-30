package com.mapbox.navigation.dropin.tripsession

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class TripSessionComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testLifecycle: TestLifecycleOwner
    private lateinit var testStore: TestStore
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: TripSessionComponent

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        mapboxNavigation = mockk(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        testStore = spyk(TestStore())
        testLifecycle = TestLifecycleOwner()
        sut = TripSessionComponent(testLifecycle.lifecycle, testStore)

        // todo this will have to be changed to `mockkStatic(PermissionsManager::class)`
        // when upgrading to Common SDK v23.2.0
        mockkObject(PermissionsManager)
        every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
        mockkStatic(LocationEngineProvider::class)
        every { LocationEngineProvider.getBestLocationEngine(any()) } returns mockk {
            every { getLastLocation(any()) } just runs
        }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `startTripSession if location permissions are granted`() =
        runBlockingTest {
            testStore.setState(
                State(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = false,
                        isReplayEnabled = false,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            testStore.setState(
                State(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = false,
                    )
                )
            )

            verify { mapboxNavigation.startTripSession() }
        }

    @Test
    fun `onDetached does not stopTripSession for a regular session`() =
        runBlockingTest {
            testStore.setState(
                State(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = false,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            sut.onDetached(mapboxNavigation)

            verify(exactly = 1) { mapboxNavigation.startTripSession() }
            verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        }

    @Test
    fun `EnableTripSession will restart a trip session when replay is enabled`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.ActiveNavigation,
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = true,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            testStore.updateState {
                it.copy(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = false,
                    )
                )
            }

            verifyOrder {
                mapboxNavigation.startReplayTripSession()
                mapboxNavigation.stopTripSession()
                mapboxNavigation.startTripSession()
            }
        }

    @Test
    fun `EnableReplayTripSession will startReplayTripSession`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.ActiveNavigation,
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = false,
                        isReplayEnabled = false,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            testStore.updateState {
                it.copy(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = true,
                    )
                )
            }

            verify { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will not startReplayTripSession without location permissions`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.ActiveNavigation,
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = false,
                        isReplayEnabled = false,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            testStore.updateState {
                it.copy(
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = false,
                        isReplayEnabled = true,
                    )
                )
            }

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will only startReplayTripSession for ActiveGuidance`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.FreeDrive,
                    tripSession = tripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = true,
                    )
                )
            )
            sut.onAttached(mapboxNavigation)
            testLifecycle.moveToState(Lifecycle.State.STARTED)

            testStore.setNavigationState(NavigationState.DestinationPreview)
            testStore.setNavigationState(NavigationState.RoutePreview)
            testStore.setNavigationState(NavigationState.Arrival)

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
            testStore.setNavigationState(NavigationState.ActiveNavigation)
            verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
        }

    private fun TestStore.setNavigationState(navState: NavigationState) {
        setState(
            state.value.copy(
                navigation = navState
            )
        )
    }

    private fun tripSessionStarterState(
        isLocationPermissionGranted: Boolean,
        isReplayEnabled: Boolean
    ): TripSessionStarterState {
        return testStore.state.value.tripSession.copy(isLocationPermissionGranted, isReplayEnabled)
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
            .also { it.currentState = Lifecycle.State.INITIALIZED }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry

        fun moveToState(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}
