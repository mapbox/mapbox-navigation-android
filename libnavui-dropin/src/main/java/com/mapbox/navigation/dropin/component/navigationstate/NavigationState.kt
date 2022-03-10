package com.mapbox.navigation.dropin.component.navigationstate

// TODO: move this to .navigation package before removing .navigationstate
sealed class NavigationState {
    object Empty : NavigationState()
    object FreeDrive : NavigationState()
    object RoutePreview : NavigationState()
    object ActiveNavigation : NavigationState()
    object Arrival : NavigationState()

    override fun toString(): String = "NavigationState.${this::class.java.simpleName}"
}
