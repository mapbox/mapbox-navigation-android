package com.mapbox.navigation.dropin.component.infopanel.freedrive

import android.annotation.SuppressLint
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelFreeDriveComponent(
    private val navigationViewContext: DropInNavigationViewContext,
    private val startNavigation: View
) : UIComponent() {

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        startNavigation.setOnClickListener {
            with(mapboxNavigation) {
                // Temporarily trigger replay here
                mapboxReplayer.clearEvents()
                resetTripSession()
                mapboxReplayer.pushRealLocation(navigationOptions.applicationContext, 0.0)
                mapboxReplayer.play()
                startReplayTripSession()

                // This will trigger the map to change ViewBinders.
                navigationViewContext.viewModel.updateState(NavigationState.ActiveNavigation)
            }
        }
    }
}
