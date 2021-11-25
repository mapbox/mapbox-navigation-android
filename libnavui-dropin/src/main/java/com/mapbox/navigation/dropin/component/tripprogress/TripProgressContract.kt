package com.mapbox.navigation.dropin.component.tripprogress

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

sealed class TripProgressAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : TripProgressAction()
}

internal sealed class TripProgressResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : TripProgressResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : TripProgressResult()
}
