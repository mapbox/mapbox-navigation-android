package com.mapbox.navigation.dropin.component.navigationstate

import com.mapbox.navigation.dropin.component.DropInViewModel

internal class NavigationStateViewModel : DropInViewModel<NavigationState, NavigationStateAction>(
    initialState = NavigationState.Empty
) {

    override suspend fun process(
        accumulator: NavigationState,
        value: NavigationStateAction
    ): NavigationState {
        return when (value) {
            NavigationStateAction.ToActiveNavigation -> NavigationState.ActiveNavigation
            NavigationStateAction.ToArrival -> NavigationState.Arrival
            NavigationStateAction.ToEmpty -> NavigationState.Empty
            NavigationStateAction.ToFreeDrive -> NavigationState.FreeDrive
            NavigationStateAction.ToRoutePreview -> NavigationState.RoutePreview
        }
    }
}
