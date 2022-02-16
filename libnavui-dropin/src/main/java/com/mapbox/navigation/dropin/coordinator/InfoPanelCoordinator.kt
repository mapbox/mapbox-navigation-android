package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
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

/**
 * Coordinator for navigation information.
 * This is also known as the bottom sheet.
 */
internal class InfoPanelCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    infoPanel: ViewGroup
) : UICoordinator<ViewGroup>(infoPanel) {

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
