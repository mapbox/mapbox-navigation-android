package com.mapbox.navigation.dropin

internal sealed class NavigationStateTransitionResult: Result {
    data class ToEmpty(val state: NavigationViewState): NavigationStateTransitionResult()

    data class ToFreeDrive(val state: NavigationViewState): NavigationStateTransitionResult()

    data class ToRoutePreview(val state: NavigationViewState): NavigationStateTransitionResult()

    data class ToActiveNavigation(val state: NavigationViewState): NavigationStateTransitionResult()

    data class ToArrival(val state: NavigationViewState): NavigationStateTransitionResult()
}
