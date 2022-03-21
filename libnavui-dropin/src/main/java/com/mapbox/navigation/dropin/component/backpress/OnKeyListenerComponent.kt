package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationView
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.lifecycle.UIComponent

/**
 * Key listener for the [DropInNavigationView].
 *
 * On back pressed will change the navigation state.
 * (FreeDrive) <- (RoutePreview) <- (ActiveNavigation)
 *                                  (Arrival)
 */
internal class OnKeyListenerComponent(
    context: DropInNavigationViewContext,
    private val view: View,
) : UIComponent() {

    private val routesViewModel = context.viewModel.routesViewModel
    private val navigationStateViewModel = context.viewModel.navigationStateViewModel
    private val destinationViewModel = context.viewModel.destinationViewModel

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleBackPress()
            } else {
                false
            }
        }
    }

    private fun handleBackPress(): Boolean {
        return when (navigationStateViewModel.state.value) {
            NavigationState.FreeDrive, NavigationState.Empty -> {
                val hasDestination = destinationViewModel.state.value.destination != null
                if (hasDestination) {
                    destinationViewModel.invoke(DestinationAction.SetDestination(null))
                }
                hasDestination
            }
            NavigationState.RoutePreview -> {
                routesViewModel.invoke(RoutesAction.SetRoutes(emptyList(), 0))
                true
            }
            NavigationState.ActiveNavigation,
            NavigationState.Arrival -> {
                routesViewModel.invoke(RoutesAction.StopNavigation)
                true
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        view.isFocusableInTouchMode = false
        view.setOnKeyListener(null)
    }
}
