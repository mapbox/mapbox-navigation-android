package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIViewModel

internal class NavigationStateViewModel(
    default: NavigationState
) : UIViewModel<NavigationState, NavigationStateAction>(default) {

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
