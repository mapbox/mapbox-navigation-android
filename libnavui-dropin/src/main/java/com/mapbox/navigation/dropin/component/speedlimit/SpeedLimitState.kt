package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitError
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitErrorFactory
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue

internal data class SpeedLimitState(
    val isVisible: Boolean,
    val navigationState: NavigationState,
    val speedLimit: Expected<UpdateSpeedLimitError, UpdateSpeedLimitValue>
) {
    companion object {
        @OptIn(ExperimentalMapboxNavigationAPI::class)
        fun initial(): SpeedLimitState = SpeedLimitState(
            isVisible = false,
            navigationState = NavigationState.Empty,
            speedLimit = ExpectedFactory.createError(
                UpdateSpeedLimitErrorFactory.buildSpeedLimitError(
                    "Speed Limit data not available",
                    null
                )
            )
        )
    }
}
