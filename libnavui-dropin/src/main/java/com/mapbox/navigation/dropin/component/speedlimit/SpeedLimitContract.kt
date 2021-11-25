package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitError
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue

sealed class SpeedLimitAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : SpeedLimitAction()

    data class UpdateLocationMatcher(
        val locationMatcher: LocationMatcherResult
    ) : SpeedLimitAction()
}

internal sealed class SpeedLimitResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : SpeedLimitResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : SpeedLimitResult()

    data class OnLocationMatcher(
        val speedLimit: Expected<UpdateSpeedLimitError, UpdateSpeedLimitValue>
    ) : SpeedLimitResult()
}
