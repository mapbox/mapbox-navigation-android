package com.mapbox.navigation.dropin

internal sealed class NavigationStateTransitionResult {
    data class ToEmpty(val state: NavigationState): NavigationStateTransitionResult()

    data class ToFreeDrive(val state: NavigationState): NavigationStateTransitionResult()

    data class ToRoutePreview(val state: NavigationState): NavigationStateTransitionResult()

    data class ToActiveNavigation(val state: NavigationState): NavigationStateTransitionResult()

    data class ToArrival(val state: NavigationState): NavigationStateTransitionResult()
}
