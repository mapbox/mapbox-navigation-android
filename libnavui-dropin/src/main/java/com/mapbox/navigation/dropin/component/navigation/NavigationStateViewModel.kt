package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class NavigationStateAction {
    data class Update(val state: NavigationState) : NavigationStateAction()
}

class NavigationStateViewModel(
    default: NavigationState
) : UIViewModel<NavigationState, NavigationStateAction>(default) {

    // TODO get destination and navigation route for initial state

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            mapboxNavigation.flowOnFinalDestinationArrival().collect {
                invoke(NavigationStateAction.Update(NavigationState.Arrival))
            }
        }
    }

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: NavigationState,
        action: NavigationStateAction
    ): NavigationState {
        return when (action) {
            is NavigationStateAction.Update -> action.state
        }
    }
}
