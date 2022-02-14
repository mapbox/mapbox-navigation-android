package com.mapbox.navigation.dropin

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.screen.ActiveGuidanceScreenBinder
import com.mapbox.navigation.dropin.binder.screen.ArrivalScreenBinder
import com.mapbox.navigation.dropin.binder.screen.EmptyScreenBinder
import com.mapbox.navigation.dropin.binder.screen.FreeDriveScreenBinder
import com.mapbox.navigation.dropin.binder.screen.RoutePreviewScreenBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Entry point for the drop in navigation flow.
 */
internal class DropInNavigationViewCoordinator(
    private val navigationViewContext: DropInNavigationViewContext
) : UICoordinator(navigationViewContext.viewGroup) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext.viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive -> FreeDriveScreenBinder(navigationViewContext)
                NavigationState.RoutePreview -> RoutePreviewScreenBinder(navigationViewContext)
                NavigationState.ActiveNavigation -> ActiveGuidanceScreenBinder(
                    navigationViewContext
                )
                NavigationState.Arrival -> ArrivalScreenBinder(navigationViewContext)
                NavigationState.Empty -> EmptyScreenBinder()
            }
        }
    }
}
