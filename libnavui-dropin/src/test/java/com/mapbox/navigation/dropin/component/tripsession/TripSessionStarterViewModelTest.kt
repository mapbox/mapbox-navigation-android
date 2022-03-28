package com.mapbox.navigation.dropin.component.tripsession

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `verify default state is respected`() = runBlockingTest {
        val navigationStateViewModel = mockNavigationStateViewModel()
        val tripSessionStarterViewModel = TripSessionStarterViewModel(
            navigationStateViewModel,
            TripSessionStarterState(isReplayEnabled = false, isLocationPermissionGranted = true)
        )

        tripSessionStarterViewModel.onAttached(mockMapboxNavigation())

        assertFalse(tripSessionStarterViewModel.state.value.isReplayEnabled)
        assertTrue(tripSessionStarterViewModel.state.value.isLocationPermissionGranted)
    }
    @Test
    fun `startTripSession if location permissions are granted`() =
        runBlockingTest {
            val tripSessionStarterViewModel = TripSessionStarterViewModel(
                mockNavigationStateViewModel(),
                TripSessionStarterState(
                    isLocationPermissionGranted = false,
                    isReplayEnabled = false
                )
            )
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.invoke(
                TripSessionStarterAction.OnLocationPermission(true)
            )

            verify { mapboxNavigation.startTripSession() }
        }

    @Test
    fun `onDetached does not stopTripSession for a regular session`() =
        runBlockingTest {
            val tripSessionStarterViewModel = TripSessionStarterViewModel(
                mockNavigationStateViewModel(),
                TripSessionStarterState(
                    isLocationPermissionGranted = true,
                    isReplayEnabled = false
                )
            )
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.onDetached(mapboxNavigation)

            verify(exactly = 1) { mapboxNavigation.startTripSession() }
            verify(exactly = 0) { mapboxNavigation.stopTripSession() }
        }

    @Test
    fun `EnableTripSession will restart a trip session when replay is enabled`() =
        runBlockingTest {
            val tripSessionStarterViewModel = TripSessionStarterViewModel(
                mockNavigationStateViewModel(NavigationState.ActiveNavigation),
                TripSessionStarterState(
                    isLocationPermissionGranted = true,
                    isReplayEnabled = true
                )
            )
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.invoke(TripSessionStarterAction.EnableTripSession)

            verifyOrder {
                mapboxNavigation.startReplayTripSession()
                mapboxNavigation.stopTripSession()
                mapboxNavigation.startTripSession()
            }
        }

    @Test
    fun `EnableReplayTripSession will startReplayTripSession`() =
        runBlockingTest {
            val navigationStateViewModel = mockNavigationStateViewModel(
                NavigationState.ActiveNavigation
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(navigationStateViewModel)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.invoke(
                TripSessionStarterAction.OnLocationPermission(true)
            )
            tripSessionStarterViewModel.invoke(TripSessionStarterAction.EnableReplayTripSession)

            verify { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will not startReplayTripSession without location permissions`() =
        runBlockingTest {
            val navigationStateViewModel = mockNavigationStateViewModel(
                NavigationState.ActiveNavigation
            )
            val tripSessionStarterViewModel = TripSessionStarterViewModel(navigationStateViewModel)
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            tripSessionStarterViewModel.invoke(
                TripSessionStarterAction.OnLocationPermission(false)
            )
            tripSessionStarterViewModel.invoke(TripSessionStarterAction.EnableReplayTripSession)

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
        }

    @Test
    fun `EnableReplayTripSession will only startReplayTripSession for ActiveGuidance`() =
        runBlockingTest {
            val stateFlow = MutableStateFlow<NavigationState>(NavigationState.FreeDrive)
            val navigationStateViewModel: NavigationStateViewModel = mockk {
                every { state } returns stateFlow
            }
            val tripSessionStarterViewModel = TripSessionStarterViewModel(
                navigationStateViewModel,
                TripSessionStarterState(isLocationPermissionGranted = true, isReplayEnabled = true)
            )
            val mapboxNavigation = mockMapboxNavigation()

            tripSessionStarterViewModel.onAttached(mapboxNavigation)
            stateFlow.emit(NavigationState.DestinationPreview)
            stateFlow.emit(NavigationState.RoutePreview)
            stateFlow.emit(NavigationState.Arrival)

            verify(exactly = 0) { mapboxNavigation.startReplayTripSession() }
            stateFlow.emit(NavigationState.ActiveNavigation)
            verify(exactly = 1) { mapboxNavigation.startReplayTripSession() }
        }

    private fun mockMapboxNavigation(): MapboxNavigation = mockk(relaxed = true)

    private fun mockNavigationStateViewModel(
        initialState: NavigationState = NavigationState.FreeDrive
    ): NavigationStateViewModel {
        return mockk {
            every { state } returns MutableStateFlow(initialState)
        }
    }
}
