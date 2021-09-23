package com.mapbox.navigation.dropin.processor

import com.mapbox.navigation.dropin.contract.ContainerVisibilityResult

internal sealed interface ContainerVisibilityProcessor {
    fun process(): ContainerVisibilityResult

    object ForEmpty : ContainerVisibilityProcessor {
        override fun process(): ContainerVisibilityResult = ContainerVisibilityResult.ForEmpty(
            volumeContainerVisible = false,
            recenterContainerVisible = false,
            maneuverContainerVisible = false,
            infoPanelContainerVisible = false,
            speedLimitContainerVisible = false,
            routeOverviewContainerVisible = false
        )
    }

    object ForFreeDrive : ContainerVisibilityProcessor {
        override fun process(): ContainerVisibilityResult = ContainerVisibilityResult.ForFreeDrive(
            volumeContainerVisible = false,
            recenterContainerVisible = true,
            maneuverContainerVisible = false,
            infoPanelContainerVisible = true,
            speedLimitContainerVisible = true,
            routeOverviewContainerVisible = false
        )
    }

    object ForRoutePreview : ContainerVisibilityProcessor {
        override fun process(): ContainerVisibilityResult =
            ContainerVisibilityResult.ForRoutePreview(
                volumeContainerVisible = false,
                recenterContainerVisible = true,
                maneuverContainerVisible = false,
                infoPanelContainerVisible = true,
                speedLimitContainerVisible = false,
                routeOverviewContainerVisible = true
            )
    }

    object ForActiveNavigation : ContainerVisibilityProcessor {
        override fun process(): ContainerVisibilityResult =
            ContainerVisibilityResult.ForActiveNavigation(
                volumeContainerVisible = true,
                recenterContainerVisible = true,
                maneuverContainerVisible = true,
                infoPanelContainerVisible = true,
                speedLimitContainerVisible = true,
                routeOverviewContainerVisible = true
            )
    }

    object ForArrival : ContainerVisibilityProcessor {
        override fun process(): ContainerVisibilityResult = ContainerVisibilityResult.ForArrival(
            volumeContainerVisible = true,
            recenterContainerVisible = true,
            maneuverContainerVisible = true,
            infoPanelContainerVisible = true,
            speedLimitContainerVisible = false,
            routeOverviewContainerVisible = true
        )
    }
}
