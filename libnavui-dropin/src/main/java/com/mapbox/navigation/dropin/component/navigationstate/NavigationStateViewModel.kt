package com.mapbox.navigation.dropin.component.navigationstate

import com.mapbox.navigation.dropin.component.DropInViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState

internal class NavigationStateViewModel : DropInViewModel<NavigationState, NavigationStateAction>(
    initialState = NavigationState.FreeDrive
) {

    override suspend fun process(
        accumulator: NavigationState,
        value: NavigationStateAction
    ): NavigationState {
        return when (value) {
            NavigationStateAction.ToActiveNavigation -> NavigationState.ActiveNavigation
            NavigationStateAction.ToArrival -> NavigationState.Arrival
            NavigationStateAction.ToEmpty -> NavigationState.FreeDrive
            NavigationStateAction.ToFreeDrive -> NavigationState.FreeDrive
            NavigationStateAction.ToRoutePreview -> NavigationState.RoutePreview
        }
    }
}
