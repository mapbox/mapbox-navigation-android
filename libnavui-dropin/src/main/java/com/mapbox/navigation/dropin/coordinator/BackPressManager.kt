package com.mapbox.navigation.dropin.coordinator

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Destination
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Class that manages onBackPressedCallback enabled state
 * and handles onBackPressed event for each NavigationState.

 * It enables/disables onBackPressedCallback when [Destination] is set and moves
 * [NavigationStateManager] FSM backwards by clearing each source of truth.
 * ```
 * (FreeDrive) <- (RoutePreview) <- (ActiveNavigation)
 *                                  (Arrival)
 * ```
 */
internal class BackPressManager(
    private val context: DropInNavigationViewContext
) : UIComponent() {

    private val routesState: StateFlow<RoutesState> = context.routesState
    private val onBackPressedEvent: SharedFlow<Unit> = context.viewModel.onBackPressedEvent
    private val navigationState: StateFlow<NavigationState> = context.navigationState

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        routesState.map { it.destination }.observe { d: Destination? ->
            context.onBackPressedCallback.isEnabled = d != null
        }

        onBackPressedEvent.observe {
            when (navigationState.value) {
                NavigationState.FreeDrive -> {
                    context.dispatch(RoutesAction.SetDestination(null))
                }
                NavigationState.RoutePreview -> {
                    context.dispatch(RoutesAction.SetRoutes(emptyList(), 0))
                }
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> {
                    context.dispatch(RoutesAction.StopNavigation)
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
