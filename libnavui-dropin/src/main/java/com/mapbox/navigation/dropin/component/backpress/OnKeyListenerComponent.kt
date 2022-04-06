package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent

/**
 * Key listener for the [NavigationView].
 *
 * On back pressed will update the navigation state.
 *
 * (FreeDrive) <- (DestinationPreview) <- (RoutePreview) <- (ActiveNavigation)
 *             <- (Arrival)
 */
internal class OnKeyListenerComponent(
    private val navigationStateViewModel: NavigationStateViewModel,
    private val destinationViewModel: DestinationViewModel,
    private val routesViewModel: RoutesViewModel,
    private val view: View,
) : UIComponent() {

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
            NavigationState.FreeDrive -> {
                false
            }
            NavigationState.DestinationPreview -> {
                destinationViewModel.invoke(
                    DestinationAction.SetDestination(null)
                )
                navigationStateViewModel.invoke(
                    NavigationStateAction.Update(NavigationState.FreeDrive)
                )
                true
            }
            NavigationState.RoutePreview -> {
                routesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))
                navigationStateViewModel.invoke(
                    NavigationStateAction.Update(NavigationState.DestinationPreview)
                )
                true
            }
            NavigationState.ActiveNavigation -> {
                navigationStateViewModel.invoke(
                    NavigationStateAction.Update(NavigationState.RoutePreview)
                )
                true
            }
            NavigationState.Arrival -> {
                routesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))
                destinationViewModel.invoke(DestinationAction.SetDestination(null))
                navigationStateViewModel.invoke(
                    NavigationStateAction.Update(NavigationState.FreeDrive)
                )
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
