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

/**
 * Defines actions responsible to mutate the [TripSessionStarterState].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class TripSessionStarterAction {
    /**
     * The action informs whether the location permissions have been granted.
     * @param granted is set to true if location permissions were granted; false otherwise
     */
    data class OnLocationPermission(val granted: Boolean) : TripSessionStarterAction()

    /**
     * The action enables trip session based on real gps updates.
     */
    object EnableTripSession : TripSessionStarterAction()

    /**
     * The action enables replay trip session based on simulated gps updates.
     */
    object EnableReplayTripSession : TripSessionStarterAction()
}

/**
 * The class is responsible to start and stop the `TripSession` for NavigationView.
 * @param navigationStateViewModel defines the current screen state
 * @param initialState defines the initial [TripSessionStarterState]
 */
@ExperimentalPreviewMapboxNavigationAPI
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

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
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

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     * @param mapboxNavigation
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.stop(mapboxNavigation)
        replayRouteTripSession = null
        super.onDetached(mapboxNavigation)
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
}
