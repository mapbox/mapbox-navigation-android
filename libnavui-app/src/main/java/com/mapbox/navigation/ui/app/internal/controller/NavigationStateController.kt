package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateMachine
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The class is responsible to set the screen to one of the [NavigationState] based on the
 * [NavigationStateAction] received.
 * @param store the default [NavigationState]
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

    // TODO get destination and navigation route for initial state

    /**
     * Signals that the [mapboxNavigation] instance is ready for use.
     * @param mapboxNavigation
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        NavigationStateMachine(
            initialState = store.state.value.navigation,
            destinationFlow = store.slice(coroutineScope) { it.destination },
            previewRoutesFlow = store.slice(coroutineScope) {
                if (it.previewRoutes is RoutePreviewState.Ready) it.previewRoutes.routes
                else emptyList()
            },
            navigationRoutesFlow = mapboxNavigation.navigationRoutesStateFlow(),
            onArrivalSignal = mapboxNavigation.flowOnFinalDestinationArrival()
        ).navigationState(coroutineScope)
            .filter { store.state.value.navigation != it }
            .observe {
                logD("NavStateController") {
                    "UPDATE NavigationState: ${store.state.value.navigation} -> $it"
                }
                store.dispatch(NavigationStateAction.Update(it))
            }
    }

    override fun process(state: State, action: Action): State {
        if (action is NavigationStateAction) {
            return state.copy(
                navigation = processNavigationAction(state.navigation, action)
            )
        }
        return state
    }

    private fun processNavigationAction(
        state: NavigationState,
        action: NavigationStateAction
    ): NavigationState {
        return when (action) {
            is NavigationStateAction.Update -> action.state
        }
    }

    private fun MapboxNavigation.navigationRoutesStateFlow(): StateFlow<List<NavigationRoute>> =
        flowRoutesUpdated()
            .map { it.navigationRoutes }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), getNavigationRoutes())
}
