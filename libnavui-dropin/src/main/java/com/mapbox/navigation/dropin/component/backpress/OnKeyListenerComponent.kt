package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

/**
 * Key listener for the [NavigationView].
 *
 * On back pressed will update the navigation state.
 *
 * (FreeDrive) <- (DestinationPreview) <- (RoutePreview) <- (ActiveNavigation)
 *             <- (Arrival)
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class OnKeyListenerComponent(
    private val store: Store,
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
        return when (store.state.value.navigation) {
            NavigationState.FreeDrive -> {
                false
            }
            NavigationState.DestinationPreview -> {
                store.dispatch(DestinationAction.SetDestination(null))
                store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
                true
            }
            NavigationState.RoutePreview -> {
                store.dispatch(RoutesAction.SetRoutes(emptyList()))
                store.dispatch(NavigationStateAction.Update(NavigationState.DestinationPreview))
                true
            }
            NavigationState.ActiveNavigation -> {
                store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
                true
            }
            NavigationState.Arrival -> {
                store.dispatch(RoutesAction.SetRoutes(emptyList()))
                store.dispatch(DestinationAction.SetDestination(null))
                store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
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
