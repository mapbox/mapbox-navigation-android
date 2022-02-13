package com.mapbox.navigation.dropin

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.ActiveGuidanceInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.ArrivalInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.EmptyInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.FreeDriveInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.RoutePreviewInfoPanelBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DropInInfoPanelCoordinator(
    private val navigationViewContext: DropInNavigationViewContext
) : UICoordinator(navigationViewContext.infoPanelViewGroup) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext.viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive -> FreeDriveInfoPanelBinder(navigationViewContext)
                NavigationState.RoutePreview -> RoutePreviewInfoPanelBinder(navigationViewContext)
                NavigationState.ActiveNavigation -> ActiveGuidanceInfoPanelBinder(
                    navigationViewContext
                )
                NavigationState.Arrival -> ArrivalInfoPanelBinder(navigationViewContext)
                NavigationState.Empty -> EmptyInfoPanelBinder()
            }
        }
    }
}
