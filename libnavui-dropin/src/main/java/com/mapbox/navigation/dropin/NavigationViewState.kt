package com.mapbox.navigation.dropin

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue

internal sealed class NavigationViewState {
    abstract val navigationState: NavigationState
    abstract val volumeContainerVisible: Boolean
    abstract val recenterContainerVisible: Boolean
    abstract val maneuverContainerVisible: Boolean
    abstract val infoPanelContainerVisible: Boolean
    abstract val speedLimitContainerVisible: Boolean
    abstract val routeOverviewContainerVisible: Boolean

    data class UponEmpty(
        override val navigationState: NavigationState = NavigationState.Empty,
        override val volumeContainerVisible: Boolean = false,
        override val recenterContainerVisible: Boolean = false,
        override val maneuverContainerVisible: Boolean = false,
        override val infoPanelContainerVisible: Boolean = false,
        override val speedLimitContainerVisible: Boolean = false,
        override val routeOverviewContainerVisible: Boolean = false,
    ) : NavigationViewState()

    data class UponFreeDrive(
        override val navigationState: NavigationState = NavigationState.FreeDrive,
        override val volumeContainerVisible: Boolean = false,
        override val recenterContainerVisible: Boolean = true,
        override val maneuverContainerVisible: Boolean = false,
        override val infoPanelContainerVisible: Boolean = false,
        override val speedLimitContainerVisible: Boolean = true,
        override val routeOverviewContainerVisible: Boolean = false,
    ) : NavigationViewState()

    data class UponRoutePreview(
        override val navigationState: NavigationState = NavigationState.RoutePreview,
        override val volumeContainerVisible: Boolean = false,
        override val recenterContainerVisible: Boolean = true,
        override val maneuverContainerVisible: Boolean = false,
        override val infoPanelContainerVisible: Boolean = false,
        override val speedLimitContainerVisible: Boolean = false,
        override val routeOverviewContainerVisible: Boolean = true,
        val routes: List<DirectionsRoute>
    ) : NavigationViewState()

    data class UponActiveNavigation(
        override val navigationState: NavigationState = NavigationState.ActiveNavigation,
        override val volumeContainerVisible: Boolean = true,
        override val recenterContainerVisible: Boolean = true,
        override val maneuverContainerVisible: Boolean = true,
        override val infoPanelContainerVisible: Boolean = true,
        override val speedLimitContainerVisible: Boolean = true,
        override val routeOverviewContainerVisible: Boolean = true,
        val volume: Float,
        val maneuvers: List<Maneuver>,
        val speedLimit: UpdateSpeedLimitValue,
        val tripProgress: TripProgressUpdateValue,
    ) : NavigationViewState()

    data class UponArrival(
        override val navigationState: NavigationState = NavigationState.Arrival,
        override val volumeContainerVisible: Boolean = false,
        override val recenterContainerVisible: Boolean = true,
        override val maneuverContainerVisible: Boolean = true,
        override val infoPanelContainerVisible: Boolean = true,
        override val speedLimitContainerVisible: Boolean = false,
        override val routeOverviewContainerVisible: Boolean = true,
    ) : NavigationViewState()
}
