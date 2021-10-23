package com.mapbox.navigation.dropin

sealed class NavigationState {

    object Empty: NavigationState()
    object FreeDrive: NavigationState()
    object RoutePreview: NavigationState()
    object ActiveNavigation: NavigationState()
    object Arrival: NavigationState()
}
