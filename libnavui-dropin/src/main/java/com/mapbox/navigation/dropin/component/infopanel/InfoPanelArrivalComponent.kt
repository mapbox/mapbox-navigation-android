package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent

internal class InfoPanelArrivalComponent(
    private val viewModel: DropInNavigationViewModel,
    private val arrivedText: View,
    private val tripProgressView: View
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        viewModel.navigationState.observe {
            val isVisible = it == NavigationState.Arrival
            arrivedText.isVisible = isVisible
            tripProgressView.isVisible = !isVisible
        }
    }
}
