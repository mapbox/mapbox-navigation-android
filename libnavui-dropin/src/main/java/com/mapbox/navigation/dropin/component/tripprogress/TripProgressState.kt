package com.mapbox.navigation.dropin.component.tripprogress

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal data class TripProgressState(
    val isVisible: Boolean,
    val navigationState: NavigationState
) {
    companion object {
        fun initial(): TripProgressState =
            TripProgressState(
                isVisible = false,
                navigationState = NavigationState.Empty
            )
    }
}
