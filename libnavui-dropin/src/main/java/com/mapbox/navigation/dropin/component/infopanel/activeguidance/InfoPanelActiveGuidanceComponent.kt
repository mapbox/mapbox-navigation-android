package com.mapbox.navigation.dropin.component.infopanel.activeguidance

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent

internal class InfoPanelActiveGuidanceComponent(
    private val navigationViewContext: DropInNavigationViewContext,
    private val endNavigation: View,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        endNavigation.setOnClickListener {
            // Temporarily solution to move between states
            mapboxNavigation.setRoutes(emptyList())
            navigationViewContext.viewModel.updateState(NavigationState.FreeDrive)
        }
    }
}
