package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel

sealed class NavigationStateAction {
    data class Update(val state: NavigationState) : NavigationStateAction()
}

class NavigationStateViewModel(
    default: NavigationState
) : UIViewModel<NavigationState, NavigationStateAction>(default) {

    // TODO get destination and navigation route for initial state

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
