package com.mapbox.navigation.dropin.component.backstack

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Destination
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

/**
 * Class that handles onBackPressed event for each NavigationState.

 * It enables/disables back press handing when [Destination] is set and moves
 * [NavigationStateManager] FSM backwards by clearing each source of truth.
 * ```
 * (FreeDrive) <- (RoutePreview) <- (ActiveNavigation)
 *                                  (Arrival)
 * ```
 */
internal class BackPressManager(
    private val navigationStateViewModel: NavigationStateViewModel,
    private val destinationViewModel: DestinationViewModel,
    private val routesViewModel: RoutesViewModel,
) : UIComponent() {

    private val _onBackPressedEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onBackPressedEvent = _onBackPressedEvent.asSharedFlow()

    private var isEnabled = true

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        destinationViewModel.state.map { it.destination }.observe { d: Destination? ->
            isEnabled = d != null
        }

        onBackPressedEvent.observe {
            when (navigationStateViewModel.state.value) {
                NavigationState.FreeDrive -> {
                    destinationViewModel.invoke(DestinationAction.SetDestination(null))
                }
                NavigationState.RoutePreview -> {
                    routesViewModel.invoke(RoutesAction.SetRoutes(emptyList(), 0))
                }
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> {
                    routesViewModel.invoke(RoutesAction.StopNavigation)
                }
                else -> Unit
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        isEnabled = false
    }

    fun handleOnBackPressed(): Boolean {
        if (!isEnabled) return false

        _onBackPressedEvent.tryEmit(Unit)
        return true
    }
}
