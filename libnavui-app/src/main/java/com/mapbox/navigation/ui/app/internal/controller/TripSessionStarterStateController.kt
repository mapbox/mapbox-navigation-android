package com.mapbox.navigation.ui.app.internal.controller

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.tripsession.ReplayRouteTripSession
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The class is responsible to start and stop the `TripSession` for NavigationView.
 * @param store defines the current screen state
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
class TripSessionStarterStateController(
    private val store: Store
) : StateController() {
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

        flowStartReplaySession().observe { starterState ->
            when (starterState.isLocationPermissionGranted) {
                true ->
                    if (starterState.isReplayEnabled) {
                        replayRouteTripSession?.stop(mapboxNavigation)
                        replayRouteTripSession = ReplayRouteTripSession()
                        replayRouteTripSession?.start(mapboxNavigation)
                    } else {
                        replayRouteTripSession?.stop(mapboxNavigation)
                        replayRouteTripSession = null
                        mapboxNavigation.ensureTripSessionStarted()
                    }
                false ->
                    mapboxNavigation.ensureTripSessionStopped()
            }
        }
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached.
     * @param mapboxNavigation
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
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

    private fun MapboxNavigation.ensureTripSessionStarted() {
        if (getTripSessionState() != TripSessionState.STARTED) {
            startTripSession()
        }
    }

    private fun MapboxNavigation.ensureTripSessionStopped() {
        if (getTripSessionState() != TripSessionState.STOPPED) {
            stopTripSession()
        }
    }
}
