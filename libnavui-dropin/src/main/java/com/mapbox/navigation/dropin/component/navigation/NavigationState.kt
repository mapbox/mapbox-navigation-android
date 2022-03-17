package com.mapbox.navigation.dropin.component.navigation

sealed class NavigationState {
    object FreeDrive : NavigationState()
    object DestinationPreview : NavigationState()
    object RoutePreview : NavigationState()
    object ActiveNavigation : NavigationState()
    object Arrival : NavigationState()

    override fun toString(): String = "NavigationState.${this::class.java.simpleName}"
}
