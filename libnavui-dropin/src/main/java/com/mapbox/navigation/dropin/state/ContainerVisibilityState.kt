package com.mapbox.navigation.dropin.state

data class ContainerVisibilityState(
    val volumeContainerVisible: Boolean,
    val recenterContainerVisible: Boolean,
    val maneuverContainerVisible: Boolean,
    val infoPanelContainerVisible: Boolean,
    val speedLimitContainerVisible: Boolean,
    val routeOverviewContainerVisible: Boolean
) {
    companion object {
        fun initial(): ContainerVisibilityState = ContainerVisibilityState(
            volumeContainerVisible = false,
            recenterContainerVisible = false,
            maneuverContainerVisible = false,
            infoPanelContainerVisible = false,
            speedLimitContainerVisible = false,
            routeOverviewContainerVisible = false
        )
    }
}
