package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.route.FetchAndSetRouteUseCase
import javax.inject.Inject
import javax.inject.Provider

/**
 * Class that fetches and sets new route on destination change when in RoutePreview.
 */
internal class RouteManager @Inject constructor(
    private val viewModel: DropInNavigationViewModel,
    private val fetchAndSetRouteUseCaseProvider: Provider<FetchAndSetRouteUseCase>
) : UIComponent() {

    private val enabledInStates = listOf(NavigationState.RoutePreview)
    private val fetchAndSetRouteUseCase: FetchAndSetRouteUseCase
        get() = fetchAndSetRouteUseCaseProvider.get()

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
