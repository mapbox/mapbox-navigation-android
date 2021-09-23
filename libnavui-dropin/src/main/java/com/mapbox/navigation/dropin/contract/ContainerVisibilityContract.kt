package com.mapbox.navigation.dropin.contract

import com.mapbox.navigation.dropin.processor.ContainerVisibilityProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal sealed class ContainerVisibilityAction {
    object ForEmpty : ContainerVisibilityAction()
    object ForFreeDrive : ContainerVisibilityAction()
    object ForRoutePreview : ContainerVisibilityAction()
    object ForActiveNavigation : ContainerVisibilityAction()
    object ForArrival : ContainerVisibilityAction()
}

internal sealed interface ContainerVisibilityResult {
    val volumeContainerVisible: Boolean
    val recenterContainerVisible: Boolean
    val maneuverContainerVisible: Boolean
    val infoPanelContainerVisible: Boolean
    val speedLimitContainerVisible: Boolean
    val routeOverviewContainerVisible: Boolean

    data class ForEmpty(
        override val volumeContainerVisible: Boolean,
        override val recenterContainerVisible: Boolean,
        override val maneuverContainerVisible: Boolean,
        override val infoPanelContainerVisible: Boolean,
        override val speedLimitContainerVisible: Boolean,
        override val routeOverviewContainerVisible: Boolean
    ) : ContainerVisibilityResult

    data class ForFreeDrive(
        override val volumeContainerVisible: Boolean,
        override val recenterContainerVisible: Boolean,
        override val maneuverContainerVisible: Boolean,
        override val infoPanelContainerVisible: Boolean,
        override val speedLimitContainerVisible: Boolean,
        override val routeOverviewContainerVisible: Boolean
    ) : ContainerVisibilityResult

    data class ForRoutePreview(
        override val volumeContainerVisible: Boolean,
        override val recenterContainerVisible: Boolean,
        override val maneuverContainerVisible: Boolean,
        override val infoPanelContainerVisible: Boolean,
        override val speedLimitContainerVisible: Boolean,
        override val routeOverviewContainerVisible: Boolean
    ) : ContainerVisibilityResult

    data class ForActiveNavigation(
        override val volumeContainerVisible: Boolean,
        override val recenterContainerVisible: Boolean,
        override val maneuverContainerVisible: Boolean,
        override val infoPanelContainerVisible: Boolean,
        override val speedLimitContainerVisible: Boolean,
        override val routeOverviewContainerVisible: Boolean
    ) : ContainerVisibilityResult

    data class ForArrival(
        override val volumeContainerVisible: Boolean,
        override val recenterContainerVisible: Boolean,
        override val maneuverContainerVisible: Boolean,
        override val infoPanelContainerVisible: Boolean,
        override val speedLimitContainerVisible: Boolean,
        override val routeOverviewContainerVisible: Boolean
    ) : ContainerVisibilityResult
}

internal fun Flow<ContainerVisibilityAction>.toProcessor(): Flow<ContainerVisibilityProcessor> =
    map { action ->
        when (action) {
            is ContainerVisibilityAction.ForEmpty -> ContainerVisibilityProcessor.ForEmpty
            is ContainerVisibilityAction.ForFreeDrive -> ContainerVisibilityProcessor.ForFreeDrive
            is ContainerVisibilityAction.ForRoutePreview ->
                ContainerVisibilityProcessor.ForRoutePreview
            is ContainerVisibilityAction.ForActiveNavigation ->
                ContainerVisibilityProcessor.ForActiveNavigation
            is ContainerVisibilityAction.ForArrival -> ContainerVisibilityProcessor.ForArrival
        }
    }

internal fun Flow<ContainerVisibilityProcessor>.toResult(): Flow<ContainerVisibilityResult> =
    map { processor ->
        when (processor) {
            is ContainerVisibilityProcessor.ForEmpty -> processor.process()
            is ContainerVisibilityProcessor.ForFreeDrive -> processor.process()
            is ContainerVisibilityProcessor.ForRoutePreview -> processor.process()
            is ContainerVisibilityProcessor.ForActiveNavigation -> processor.process()
            is ContainerVisibilityProcessor.ForArrival -> processor.process()
        }
    }
