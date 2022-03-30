package com.mapbox.navigation.dropin.component.tripsession

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class TripSessionStarterAction {
    data class OnLocationPermission(val granted: Boolean) : TripSessionStarterAction()
    object EnableTripSession : TripSessionStarterAction()
    object EnableReplayTripSession : TripSessionStarterAction()
}

data class TripSessionStarterState(
    val isLocationPermissionGranted: Boolean = false,
    // TODO this is true for development. Road testing should be set to false.
    val isReplayEnabled: Boolean = true,
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterViewModel(
    val navigationStateViewModel: NavigationStateViewModel,
    initialState: TripSessionStarterState = TripSessionStarterState(),
) : UIViewModel<TripSessionStarterState, TripSessionStarterAction>(initialState) {

    private var replayRouteTripSession: ReplayRouteTripSession? = null

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: TripSessionStarterState,
        action: TripSessionStarterAction
    ): TripSessionStarterState {
        return when (action) {
            is TripSessionStarterAction.OnLocationPermission -> {
                state.copy(isLocationPermissionGranted = action.granted)
            }
            TripSessionStarterAction.EnableReplayTripSession -> {
                state.copy(isReplayEnabled = true)
            }
            TripSessionStarterAction.EnableTripSession -> {
                state.copy(isReplayEnabled = false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            flowStartReplaySession().collect { starterState ->
                if (!starterState.isLocationPermissionGranted) {
                    mapboxNavigation.stopTripSession()
                } else if (starterState.isReplayEnabled) {
                    replayRouteTripSession?.stop(mapboxNavigation)
                    replayRouteTripSession = ReplayRouteTripSession()
                    replayRouteTripSession?.start(mapboxNavigation)
                } else {
                    replayRouteTripSession?.stop(mapboxNavigation)
                    replayRouteTripSession = null
                    mapboxNavigation.startTripSession()
                }
            }
        }
    }

    private fun flowStartReplaySession(): Flow<TripSessionStarterState> = combine(
        navigationStateViewModel.state, state
    ) { navigationState, tripSessionStarterState ->
        if (navigationState !is NavigationState.ActiveNavigation) {
            tripSessionStarterState.copy(isReplayEnabled = false)
        } else {
            tripSessionStarterState
        }
    }.distinctUntilChanged()

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.stop(mapboxNavigation)
        replayRouteTripSession = null
        super.onDetached(mapboxNavigation)
    }
}
