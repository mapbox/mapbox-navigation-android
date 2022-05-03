package com.mapbox.navigation.dropin.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The class is responsible to set the screen to one of the [NavigationState] based on the
 * [NavigationStateAction] received.
 * @param default the default [NavigationState] to start with
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationStateController(
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
