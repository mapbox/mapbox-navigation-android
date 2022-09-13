package com.mapbox.navigation.dropin.component.backpress

import android.view.KeyEvent
import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
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
    private val delegateOnKeyListener: View.OnKeyListener? = null
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { v, keyCode, event ->
            if (delegateOnKeyListener?.onKey(v, keyCode, event) == true) {
                return@setOnKeyListener true
            }
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
                store.dispatch(RoutePreviewAction.Ready(emptyList()))
                store.dispatch(NavigationStateAction.Update(NavigationState.DestinationPreview))
                true
            }
            NavigationState.ActiveNavigation -> {
                store.dispatch(RoutesAction.SetRoutes(emptyList()))
                store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
                true
            }
            NavigationState.Arrival -> {
                store.dispatch(endNavigation())
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
