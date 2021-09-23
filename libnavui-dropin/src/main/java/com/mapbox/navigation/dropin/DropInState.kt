package com.mapbox.navigation.dropin

sealed class DropInState {

    object Empty
    object FreeDrive
    object RoutePreview
    object ActiveNavigation
    object Arrival
}
