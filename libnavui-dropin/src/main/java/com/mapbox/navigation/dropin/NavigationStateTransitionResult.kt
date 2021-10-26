package com.mapbox.navigation.dropin

internal sealed class NavigationStateTransitionResult {
    abstract val state: NavigationState

    data class ToEmpty(
        override val state: NavigationState = NavigationState.Empty
    ) : NavigationStateTransitionResult()

    data class ToFreeDrive(
        override val state: NavigationState = NavigationState.FreeDrive
    ) : NavigationStateTransitionResult()

    data class ToRoutePreview(
        override val state: NavigationState = NavigationState.RoutePreview
    ) : NavigationStateTransitionResult()

    data class ToActiveNavigation(
        override val state: NavigationState = NavigationState.ActiveNavigation
    ) : NavigationStateTransitionResult()

    data class ToArrival(
        override val state: NavigationState = NavigationState.Arrival
    ) : NavigationStateTransitionResult()
}
