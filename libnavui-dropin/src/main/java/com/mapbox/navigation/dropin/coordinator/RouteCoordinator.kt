package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.route.FetchAndSetRouteUseCase

/**
 * Coordinator that fetches and sets new route on destination change when in RoutePreview.
 */
internal class RouteCoordinator(
    private val context: DropInNavigationViewContext
) : UIComponent() {

    private val viewModel = context.viewModel
    private val fetchAndSetRouteUseCase: FetchAndSetRouteUseCase by lazy {
        context.fetchAndSetRouteUseCase()
    }
    private val enabledInStates = listOf(NavigationState.RoutePreview)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        viewModel.destination.observe { destination ->
            val navState = viewModel.navigationState.value
            if (enabledInStates.contains(navState) && destination != null) {
                fetchAndSetRouteUseCase(destination.point)
            }
        }
    }
}
