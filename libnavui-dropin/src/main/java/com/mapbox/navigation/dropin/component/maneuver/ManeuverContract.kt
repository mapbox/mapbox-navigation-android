package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

sealed class ManeuverAction {
    data class UpdateNavigationState(
        val navigationState: NavigationState
    ) : ManeuverAction()

    data class UpdateRouteProgress(
        val routeProgress: RouteProgress
    ) : ManeuverAction()
}

internal sealed class ManeuverResult {
    data class OnNavigationState(
        val navigationState: NavigationState
    ) : ManeuverResult()

    data class OnVisibility(
        val isVisible: Boolean
    ) : ManeuverResult()

    data class OnRouteProgress(
        val maneuver: Expected<ManeuverError, List<Maneuver>>
    ) : ManeuverResult()
}
