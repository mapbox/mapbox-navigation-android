package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi

internal sealed interface SpeedLimitProcessor {

    fun process(): SpeedLimitResult

    data class ProcessNavigationState(val navigationState: NavigationState) : SpeedLimitProcessor {
        override fun process(): SpeedLimitResult.OnNavigationState =
            SpeedLimitResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessVisibility(val navigationState: NavigationState) : SpeedLimitProcessor {
        private val visibilitySet = setOf(
            NavigationState.FreeDrive,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival
        )
        override fun process(): SpeedLimitResult.OnVisibility =
            SpeedLimitResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState)
            )
    }

    data class ProcessLocationMatcher(
        val locationMatcher: LocationMatcherResult,
        val speedLimitApi: MapboxSpeedLimitApi
    ) : SpeedLimitProcessor {
        override fun process(): SpeedLimitResult.OnLocationMatcher {
            val speedLimit = speedLimitApi.updateSpeedLimit(locationMatcher.speedLimit)
            return SpeedLimitResult.OnLocationMatcher(speedLimit = speedLimit)
        }
    }
}
