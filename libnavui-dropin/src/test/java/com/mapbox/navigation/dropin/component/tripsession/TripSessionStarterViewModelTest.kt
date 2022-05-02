package com.mapbox.navigation.dropin.component.tripsession

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testStore: TestStore

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        testStore = spyk(TestStore())
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
                    tripSession = TripSessionStarterState(
                        isLocationPermissionGranted = false,
                        isReplayEnabled = false,
                    )
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(true)
            )

            verify { mapboxNavigation.startTripSession() }
        }

    @Test
    fun `onDetached does not stopTripSession for a regular session`() =
        runBlockingTest {
            testStore.setState(
                State(
                    tripSession = TripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = false,
                    )
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.onDetached(mapboxNavigation)

            verify(exactly = 1) { mapboxNavigation.startTripSession() }
            verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        }

    @Test
    fun `EnableTripSession will restart a trip session when replay is enabled`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.ActiveNavigation,
                    tripSession = TripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = true,
                    )
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            testStore.dispatch(TripSessionStarterAction.EnableTripSession)

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
                    navigation = NavigationState.ActiveNavigation
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(true)
            )
            testStore.dispatch(TripSessionStarterAction.EnableReplayTripSession)

            verify { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will not startReplayTripSession without location permissions`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.ActiveNavigation,
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            testStore.dispatch(
                TripSessionStarterAction.OnLocationPermission(false)
            )
            testStore.dispatch(TripSessionStarterAction.EnableReplayTripSession)

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will only startReplayTripSession for ActiveGuidance`() =
        runBlockingTest {
            testStore.setState(
                State(
                    navigation = NavigationState.FreeDrive,
                    tripSession = TripSessionStarterState(
                        isLocationPermissionGranted = true,
                        isReplayEnabled = true,
                    )
                )
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(testStore)

            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            testStore.setNavigationState(NavigationState.DestinationPreview)
            testStore.setNavigationState(NavigationState.RoutePreview)
            testStore.setNavigationState(NavigationState.Arrival)

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
            testStore.setNavigationState(NavigationState.ActiveNavigation)
            verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
        }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }

    private fun TestStore.setNavigationState(navState: NavigationState) {
        setState(
            state.value.copy(
                navigation = navState
            )
        )
    }
}
