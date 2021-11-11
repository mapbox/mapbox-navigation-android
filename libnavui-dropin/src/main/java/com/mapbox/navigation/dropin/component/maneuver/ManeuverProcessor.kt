package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

internal sealed interface ManeuverProcessor {

    fun process(): ManeuverResult

    data class ProcessNavigationState(val navigationState: NavigationState) : ManeuverProcessor {

        override fun process(): ManeuverResult.OnNavigationState =
            ManeuverResult.OnNavigationState(
                navigationState = navigationState
            )
    }

    data class ProcessVisibility(
        val navigationState: NavigationState,
        val maneuver: Expected<ManeuverError, List<Maneuver>>
    ) : ManeuverProcessor {
        private val visibilitySet = setOf(
            NavigationState.ActiveNavigation,
            NavigationState.Arrival
        )
        override fun process(): ManeuverResult.OnVisibility =
            ManeuverResult.OnVisibility(
                isVisible = visibilitySet.contains(navigationState) && maneuver.isValue
            )
    }

    data class ProcessRouteProgress(
        val routeProgress: RouteProgress,
        val maneuverApi: MapboxManeuverApi
    ) : ManeuverProcessor {
        override fun process(): ManeuverResult.OnRouteProgress {
            val maneuver = maneuverApi.getManeuvers(routeProgress)
            return ManeuverResult.OnRouteProgress(maneuver = maneuver)
        }
    }
}
