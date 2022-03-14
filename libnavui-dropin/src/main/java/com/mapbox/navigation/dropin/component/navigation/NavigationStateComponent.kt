package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Destination
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * This component does not have a UI element. It watches navigation and route state to decide
 * what the current navigation state is.
 *
 * ```
 *  [ FreeDrive ]━━( destination != null )━━━━━━>>[ RoutePreview ]<<━━━━━━━━━━━━┓
 *       ^         ( & routes not empty  )               ┃                      ┃
 *       ┃                                    (navigationStarted=true)          ┃
 *       ┃                                               ┃                      ┃
 *       ┃                                               ┃         (navigationStarted=false)
 *       ┣━━━━( destination == null )━━━━┓               v                      ┃
 *       ┗━━━━( routes empty        )━━━━╋━━━━━[ ActiveNavigation ]━━━━━━━━━━━━━┫
 *                                       ┃               ┃                      ┃
 *                                       ┃  (onFinalDestinationArrival())       ┃
 *                                       ┃               ┃                      ┃
 *                                       ┃               v                      ┃
 *                                       ┗━━━━━━━━━━[ Arrival ]━━━━━━━━━━━━━━━━━┛
 * ```
 */
internal class NavigationStateComponent(
    private val navigationStateViewModel: NavigationStateViewModel,
    private val destinationViewModel: DestinationViewModel,
    private val routesViewModel: RoutesViewModel,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            combine(
                destinationViewModel.state,
                routesViewModel.state
            ) { destinationState, routesState ->
                getNavigationState(
                    destinationState.destination,
                    routesState.navigationStarted,
                    mapboxNavigation.getRoutes()
                )
            }.collect { updateState(it) }
        }

        coroutineScope.launch {
            mapboxNavigation.flowRoutesUpdated().collect {
                val state = getNavigationState(
                    destinationViewModel.state.value.destination,
                    routesViewModel.state.value.navigationStarted,
                    it.routes
                )
                updateState(state)
            }
        }

        coroutineScope.launch {
            mapboxNavigation.flowOnFinalDestinationArrival().collect {
                val inActiveNav =
                    navigationStateViewModel.state.value == NavigationState.ActiveNavigation
                val arrived = it.currentState == RouteProgressState.COMPLETE
                if (inActiveNav && arrived) {
                    updateState(NavigationState.Arrival)
                }
            }
        }
    }

    private fun updateState(state: NavigationState) {
        navigationStateViewModel.invoke(NavigationStateAction.Update(state))
    }

    private fun getNavigationState(
        destination: Destination?,
        activeNavigationStarted: Boolean,
        routes: List<DirectionsRoute>
    ): NavigationState {
        return if (destination != null) {
            if (routes.isNotEmpty()) {
                val inArrivalState = navigationStateViewModel.state.value == NavigationState.Arrival
                when {
                    inArrivalState && activeNavigationStarted -> NavigationState.Arrival
                    activeNavigationStarted -> NavigationState.ActiveNavigation
                    else -> NavigationState.RoutePreview
                }
            } else {
                NavigationState.FreeDrive
            }
        } else {
            NavigationState.FreeDrive
        }
    }
}
