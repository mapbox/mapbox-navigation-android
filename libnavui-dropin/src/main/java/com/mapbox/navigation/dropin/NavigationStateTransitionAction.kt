package com.mapbox.navigation.dropin

sealed class NavigationStateTransitionAction: Action {
    data class ToEmpty(val from: NavigationState): NavigationStateTransitionAction()

    data class ToFreeDrive(val from: NavigationState): NavigationStateTransitionAction()

    data class ToRoutePreview(val from: NavigationState): NavigationStateTransitionAction()

    data class ToActiveNavigation(val from: NavigationState): NavigationStateTransitionAction()

    data class ToArrival(val from: NavigationState): NavigationStateTransitionAction()
}
