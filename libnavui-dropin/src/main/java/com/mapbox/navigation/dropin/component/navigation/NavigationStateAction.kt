package com.mapbox.navigation.dropin.component.navigation

sealed class NavigationStateAction {
    data class Update(val state: NavigationState) : NavigationStateAction()
}
