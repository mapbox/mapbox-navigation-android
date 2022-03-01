package com.mapbox.navigation.dropin.coordinator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Destination
import kotlinx.coroutines.flow.combine

/**
 * Class that manages NavigationState as following:
 *
 * ```
 *  [ FreeDrive ]━━( destination != null )━━━━━━>>[ RoutePreview ]<<━━━━━━━━━━━━┓
 *       ^         ( & routes not empty  )               ┃                      ┃
 *       ┃                                  (activeNavigationStarted=true)      ┃
 *       ┃                                               ┃                      ┃
 *       ┃                                               ┃         (activeNavigationStarted=false)
 *       ┣━━━━( destination == null )━━━━┓               v                      ┃
 *       ┗━━━━( routes empty        )━━━━╋━━━━━[ ActiveNavigation ]━━━━━━━━━━━━━┫
 *                                       ┃               ┃                      ┃
 *                                       ┃  (onFinalDestinationArrival())       ┃
 *                                       ┃               ┃                      ┃
 *                                       ┃               v                      ┃
 *                                       ┗━━━━━━━━━━[ Arrival ]━━━━━━━━━━━━━━━━━┛
 * ```
 */
internal class NavigationStateManager(
    context: DropInNavigationViewContext
) : UIComponent() {

    private val viewModel = context.viewModel

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        combine(
            viewModel.destination,
            viewModel.activeNavigationStarted
        ) { destination, activeNavigationStarted ->
            getNavigationState(destination, activeNavigationStarted, mapboxNavigation.getRoutes())
        }.observe {
            viewModel.updateState(it)
        }

        mapboxNavigation.flowRoutesUpdated().observe {
            val state = getNavigationState(
                viewModel.destination.value,
                viewModel.activeNavigationStarted.value,
                it.routes
            )
            viewModel.updateState(state)
        }

        mapboxNavigation.flowOnFinalDestinationArrival().observe {
            val inActiveNav = viewModel.navigationState.value == NavigationState.ActiveNavigation
            val arrived = it.currentState == RouteProgressState.COMPLETE
            if (inActiveNav && arrived) {
                viewModel.updateState(NavigationState.Arrival)
            }
        }
    }

    private fun getNavigationState(
        destination: Destination?,
        activeNavigationStarted: Boolean,
        routes: List<DirectionsRoute>
    ): NavigationState {
        return if (destination != null) {
            if (routes.isNotEmpty()) {
                val inArrivalState = viewModel.navigationState.value == NavigationState.Arrival
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
