package com.mapbox.navigation.dropin

internal fun processTransition(action: NavigationStateTransitionAction): NavigationStateTransitionResult {
    return when(action) {
        is NavigationStateTransitionAction.ToEmpty -> {
            NavigationStateTransitionResult.ToEmpty(NavigationViewState.UponEmpty())
        }
        is NavigationStateTransitionAction.ToFreeDrive -> {
            NavigationStateTransitionResult.ToFreeDrive(
                NavigationViewState.UponFreeDrive()
            )
        }
        is NavigationStateTransitionAction.ToRoutePreview -> {
            NavigationStateTransitionResult.ToRoutePreview(
                NavigationViewState.UponRoutePreview(listOf())
            )
        }
        is NavigationStateTransitionAction.ToActiveNavigation -> {
            NavigationStateTransitionResult.ToActiveNavigation(
                TODO()
            )
        }
        is NavigationStateTransitionAction.ToArrival -> {
            NavigationStateTransitionResult.ToArrival(
                TODO()
            )
        }
    }
}
