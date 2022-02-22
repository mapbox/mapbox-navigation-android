package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent

/**
 * Coordinator that manages onBackPressedCallback enabled state
 * and handles onBackPressed event for each NavigationState.
 */
internal class BackPressCoordinator(
    private val context: DropInNavigationViewContext
) : UIComponent() {

    private val viewModel = context.viewModel
    private val stopActiveGuidanceUseCase by lazy {
        context.stopActiveGuidanceUseCase()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        viewModel.destination.observe {
            context.onBackPressedCallback.isEnabled = it != null
        }

        viewModel.onBackPressedEvent.observe {
            when (viewModel.navigationState.value) {
                NavigationState.FreeDrive -> {
                    viewModel.updateDestination(null)
                }
                NavigationState.RoutePreview -> {
                    mapboxNavigation.setRoutes(emptyList())
                }
                NavigationState.ActiveNavigation -> {
                    stopActiveGuidanceUseCase(Unit)
                }
                else -> Unit
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        context.onBackPressedCallback.isEnabled = false
    }
}
