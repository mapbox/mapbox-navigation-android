package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Defines actions responsible to mutate the [NavigationState].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class NavigationStateAction {
    /**
     * The action update the [NavigationState] for the screen.
     * @property state to update the screen to
     */
    data class Update(val state: NavigationState) : NavigationStateAction()
}

/**
 * The class is responsible to set the screen to one of the [NavigationState] based on the
 * [NavigationStateAction] received.
 * @param default the default [NavigationState] to start with
 */
@ExperimentalPreviewMapboxNavigationAPI
class NavigationStateViewModel(
    default: NavigationState
) : UIViewModel<NavigationState, NavigationStateAction>(default) {

    // TODO get destination and navigation route for initial state

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            mapboxNavigation.flowOnFinalDestinationArrival().collect {
                invoke(NavigationStateAction.Update(NavigationState.Arrival))
            }
        }
    }

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: NavigationState,
        action: NavigationStateAction
    ): NavigationState {
        return when (action) {
            is NavigationStateAction.Update -> action.state
        }
    }
}
