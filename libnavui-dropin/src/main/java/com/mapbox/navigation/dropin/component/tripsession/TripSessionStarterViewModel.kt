package com.mapbox.navigation.dropin.component.tripsession

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.Reducer
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The class is responsible to start and stop the `TripSession` for NavigationView.
 * @param navigationStateViewModel defines the current screen state
 * @param initialState defines the initial [TripSessionStarterState]
 */
@ExperimentalPreviewMapboxNavigationAPI
@SuppressLint("MissingPermission")
internal class TripSessionStarterViewModel(
    val store: Store
) : UIComponent(), Reducer {
    init {
        store.register(this)
    }

    private var replayRouteTripSession: ReplayRouteTripSession? = null

    override fun process(state: State, action: Action): State {
        if (action is TripSessionStarterAction) {
            return state.copy(
                tripSession = processTripSessionAction(state.tripSession, action)
            )
        }
        return state
    }

    private fun processTripSessionAction(
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

        coroutineScope.launch {
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
        store.select { it.navigation },
        store.select { it.tripSession }
    ) { navigationState, tripSessionStarterState ->
        if (navigationState !is NavigationState.ActiveNavigation) {
            tripSessionStarterState.copy(isReplayEnabled = false)
        } else {
            tripSessionStarterState
        }
    }.distinctUntilChanged()
}
