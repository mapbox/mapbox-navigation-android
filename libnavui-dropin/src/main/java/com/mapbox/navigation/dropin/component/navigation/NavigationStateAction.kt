package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

sealed class NavigationStateAction {
    data class Update(val state: NavigationState) : NavigationStateAction()
}
