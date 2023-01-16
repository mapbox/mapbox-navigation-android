package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The class is responsible to set the screen to one of the [NavigationState] based on the
 * [NavigationStateAction] received.
 * @param store the default [NavigationState]
 */
class NavigationStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

    // TODO get destination and navigation route for initial state

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            mapboxNavigation.flowOnFinalDestinationArrival().collect {
                store.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
            }
        }
    }

    override fun process(state: State, action: Action): State {
        if (action is NavigationStateAction) {
            return state.copy(
                navigation = processNavigationAction(state.navigation, action)
            )
        }
        return state
    }

    private fun processNavigationAction(
        state: NavigationState,
        action: NavigationStateAction
    ): NavigationState {
        return when (action) {
            is NavigationStateAction.Update -> action.state
        }
    }
}
