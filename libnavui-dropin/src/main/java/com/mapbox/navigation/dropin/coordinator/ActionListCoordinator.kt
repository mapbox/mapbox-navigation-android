package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.action.ActiveGuidanceActionBinder
import com.mapbox.navigation.dropin.binder.action.EmptyActionBinder
import com.mapbox.navigation.dropin.binder.action.FreeDriveActionBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for navigation actions.
 * This is the side panel for a portrait view.
 */
internal class ActionListCoordinator(
    private val navContext: DropInNavigationViewContext,
    actionList: ViewGroup
) : UICoordinator<ViewGroup>(actionList) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navContext.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.RoutePreview,
                NavigationState.ActiveNavigation -> ActiveGuidanceActionBinder(navContext)
                NavigationState.Arrival,
                NavigationState.FreeDrive -> FreeDriveActionBinder(navContext)
                NavigationState.Empty -> EmptyActionBinder()
            }
        }
    }
}
