package com.mapbox.navigation.ui.app.internal.controller

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterState

/**
 * The class is responsible to start and stop the `TripSession` for NavigationView.
 * @param store defines the current screen state
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
class TripSessionStarterStateController(store: Store) : StateController() {
    init {
        store.register(this)
    }

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
}
