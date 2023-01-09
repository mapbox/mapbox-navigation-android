package com.mapbox.navigation.ui.app.internal.controller

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.MapboxTripStarter
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction

/**
 * The class is responsible to start and stop the `TripSession` for NavigationView.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterStateController(store: Store) : StateController() {

    init {
        store.register(this)
    }

    private val tripStarter = MapboxTripStarter.getRegisteredInstance()

    override fun process(state: State, action: Action): State {
        if (action is TripSessionStarterAction) {
            processTripSessionAction(action)
        }
        return state
    }

    private fun processTripSessionAction(
        action: TripSessionStarterAction
    ) {
        when (action) {
            is TripSessionStarterAction.RefreshLocationPermissions -> {
                tripStarter.refreshLocationPermissions()
            }
            TripSessionStarterAction.EnableReplayTripSession -> {
                tripStarter.enableReplayRoute()
            }
            TripSessionStarterAction.EnableTripSession -> {
                tripStarter.enableMapMatching()
            }
        }
    }
}
