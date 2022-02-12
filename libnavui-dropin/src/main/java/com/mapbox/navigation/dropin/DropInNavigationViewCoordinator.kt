package com.mapbox.navigation.dropin

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.dropin.lifecycle.DropInViewCoordinator
import com.mapbox.navigation.dropin.statebinder.ActiveGuidanceViewBinder
import com.mapbox.navigation.dropin.statebinder.ArrivalViewBinder
import com.mapbox.navigation.dropin.statebinder.EmptyViewBinder
import com.mapbox.navigation.dropin.statebinder.FreeDriveViewBinder
import com.mapbox.navigation.dropin.statebinder.RoutePreviewViewBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Entry point for the drop in navigation flow.
 */
internal class DropInNavigationViewCoordinator(
    private val navigationViewContext: DropInNavigationViewContext
) : DropInViewCoordinator(navigationViewContext.viewGroup) {

    override fun MapboxNavigation.flowViewBinders(): Flow<DropInViewBinder> {
        return navigationViewContext.viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive -> FreeDriveViewBinder(navigationViewContext)
                NavigationState.RoutePreview -> RoutePreviewViewBinder(navigationViewContext)
                NavigationState.ActiveNavigation -> ActiveGuidanceViewBinder(navigationViewContext)
                NavigationState.Arrival -> ArrivalViewBinder(navigationViewContext)
                NavigationState.Empty -> EmptyViewBinder()
            }
        }
    }
}
