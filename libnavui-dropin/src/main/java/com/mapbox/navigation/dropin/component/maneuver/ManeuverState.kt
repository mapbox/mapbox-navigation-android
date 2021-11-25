package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverErrorFactory

internal data class ManeuverState(
    val isVisible: Boolean,
    val navigationState: NavigationState,
    val maneuver: Expected<ManeuverError, List<Maneuver>>
) {
    companion object {
        @OptIn(ExperimentalMapboxNavigationAPI::class)
        fun initial(): ManeuverState =
            ManeuverState(
                isVisible = false,
                navigationState = NavigationState.Empty,
                maneuver = ExpectedFactory.createError(
                    ManeuverErrorFactory.buildManeuverError(
                        errorMessage = "",
                        throwable = null
                    )
                )
            )
    }
}
