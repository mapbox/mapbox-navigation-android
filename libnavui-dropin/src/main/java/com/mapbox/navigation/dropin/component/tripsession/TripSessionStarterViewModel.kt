package com.mapbox.navigation.dropin.component.tripsession

import android.annotation.SuppressLint
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class TripSessionStarterAction {
    object EnableTripSession : TripSessionStarterAction()
    object EnableReplayTripSession : TripSessionStarterAction()
}

data class TripSessionStarterState(
    // TODO this is true for development. Road testing should be set to false.
    val isReplayEnabled: Boolean = true,
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
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
            TripSessionStarterAction.EnableReplayTripSession -> {
                state.copy(isReplayEnabled = true)
            }
            TripSessionStarterAction.EnableTripSession -> {
                state.copy(isReplayEnabled = false)
            }
        }.also {
            logI("TripSessionStarterViewModel", "State changed to $it")
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            flowStartReplaySession().collect { startReplay ->
                if (startReplay) {
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

    private fun flowStartReplaySession(): Flow<Boolean> = combine(
        navigationStateViewModel.state, state
    ) { navigationState, tripSessionStarterState ->
        navigationState is NavigationState.ActiveNavigation &&
            tripSessionStarterState.isReplayEnabled
    }.distinctUntilChanged()

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        replayRouteTripSession?.stop(mapboxNavigation)
        replayRouteTripSession = null
        super.onDetached(mapboxNavigation)
    }
}
