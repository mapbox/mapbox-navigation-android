package com.mapbox.navigation.dropin.component.tripprogress

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState

internal sealed interface TripProgressProcessor {
    fun process(): TripProgressResult

    data class ProcessNavigationState(
        val navigationState: NavigationState
    ) : TripProgressProcessor {
        override fun process(): TripProgressResult.OnNavigationState =
            TripProgressResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessVisibility(
        val navigationState: NavigationState
    ) : TripProgressProcessor {
        private val visibilitySet = setOf(
            NavigationState.ActiveNavigation,
            NavigationState.Arrival
        )
        override fun process(): TripProgressResult.OnVisibility =
            TripProgressResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState)
            )
    }
}
