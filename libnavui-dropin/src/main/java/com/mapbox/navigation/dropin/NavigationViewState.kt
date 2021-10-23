package com.mapbox.navigation.dropin

import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue

internal sealed class NavigationViewState: State {
    data class UponEmpty(
        val navigationState: NavigationState = NavigationState.Empty
    ): NavigationViewState()

    data class UponFreeDrive(
        val navigationState: NavigationState = NavigationState.FreeDrive
    ): NavigationViewState()

    data class UponRoutePreview(
        val routes: List<RouteLine>,
        val navigationState: NavigationState = NavigationState.RoutePreview
    ): NavigationViewState()

    data class UponActiveNavigation(
        val volume: Float,
        val maneuvers: List<Maneuver>,
        val speedLimit: UpdateSpeedLimitValue,
        val tripProgress: TripProgressUpdateValue,
        val navigationState: NavigationState = NavigationState.ActiveNavigation,
    ): NavigationViewState()

    data class UponArrival(
        val navigationState: NavigationState = NavigationState.Arrival
    ): NavigationViewState()
}
