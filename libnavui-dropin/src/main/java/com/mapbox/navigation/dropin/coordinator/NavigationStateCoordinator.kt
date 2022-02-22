package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.combine

/**
 * Coordinator that updates NavigationState as following:
 *
 * ```
 *  [ FreeDrive ]━━( destination != null )━━━━>>[ RoutePreview ]<<━━━━━━━━━━━━┓
 *       ^         ( & routes not empty  )             ┃                      ┃
 *       ┃                                (activeNavigationStarted=true)      ┃
 *       ┃                                             ┃                      ┃
 *       ┃                                             ┃         (activeNavigationStarted=false)
 *       ┣━━━━( destination == null )━━━━┓             v                      ┃
 *       ┗━━━━( routes empty        )━━━━┻━━━[ ActiveNavigation ] ━━━━━━━━━━━━┛
 * ```
 */
internal class NavigationStateCoordinator(
    context: DropInNavigationViewContext
) : UIComponent() {

    private val viewModel = context.viewModel

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        // TODO: add logic for NavigationState.Arrival
        combine(
            viewModel.destination,
            viewModel.activeNavigationStarted,
            mapboxNavigation.flowRoutesUpdated()
        ) { destination, activeNavigationStarted, routesUpdatedResult ->
            if (destination != null && routesUpdatedResult.routes.isNotEmpty()) {
                if (activeNavigationStarted) NavigationState.ActiveNavigation
                else NavigationState.RoutePreview
            } else {
                NavigationState.FreeDrive
            }
        }.observe {
            viewModel.updateState(it)
        }
    }
}
