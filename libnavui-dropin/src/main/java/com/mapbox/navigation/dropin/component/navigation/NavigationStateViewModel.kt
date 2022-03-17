package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel

class NavigationStateViewModel(
    default: NavigationState
) : UIViewModel<NavigationState, NavigationStateAction>(default) {

    // TODO get destination and navigation route for initial state
//    override fun onAttached(mapboxNavigation: MapboxNavigation) {
//        super.onAttached(mapboxNavigation)
//
//    }

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
