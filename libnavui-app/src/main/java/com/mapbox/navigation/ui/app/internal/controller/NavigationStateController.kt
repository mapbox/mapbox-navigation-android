package com.mapbox.navigation.ui.app.internal.controller

import android.util.Log
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
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

        mapboxNavigation.flowOnFinalDestinationArrival().observe {
            store.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
        }

        combine(
            store.select { it.destination },
            store.select { it.previewRoutes }.map {
                if (it is RoutePreviewState.Ready) it.routes
                else emptyList()
            },
            mapboxNavigation.navigationRoutesStateFlow()
        ) { destination, previewRoutes, navigationRoutes ->
            when {
                navigationRoutes.isNotEmpty() -> NavigationState.ActiveNavigation
                previewRoutes.isNotEmpty() -> NavigationState.RoutePreview
                destination != null -> NavigationState.DestinationPreview
                else -> NavigationState.FreeDrive
            }
        }
            .onEach {
                Log.d(
                    "NavStateController",
                    "RESOLVE NavigationState: ${store.state.value.navigation} -?-> $it"
                )
            }
            .filter { store.state.value.navigation != it }
            .observe {
                Log.d(
                    "NavStateController",
                    "UPDATE NavigationState: ${store.state.value.navigation} -> $it"
                )
                store.dispatch(NavigationStateAction.Update(it))
            }
    }

    private fun MapboxNavigation.navigationRoutesStateFlow(): StateFlow<List<NavigationRoute>> =
        flowRoutesUpdated()
            .map { it.navigationRoutes }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), getNavigationRoutes())

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
}

//
// given:
//   routes = MapboxNavigation.navigationRoutes
//
//               ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
//               ┃ ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓                                 ┃
//               ┃ ┃                                                    ┃                                 ┃
//               ┃ ┃                ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┃━━━━━━━━━━━━━━━━┓                ┃
//               v v                ┃                                   ┃                v                ┃
//    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓  ┃      ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓ ┃    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓ ┃
//    ┃ FreeDrive                ┃━━┛      ┃ DestinationPreview       ┃━┛    ┃ RoutePreview             ┃━┛
//    ┃--------------------------┃━━━━━━━ >┃--------------------------┃━━━━ >┃--------------------------┃
//    ┃ destination == null      ┃━━┓      ┃ destination != null      ┃      ┃ destination != null      ┃
//    ┃ previewRoute.size == 0   ┃  ┃      ┃ previewRoute.size == 0   ┃< ━━━━┃ previewRoute.size > 0    ┃
//    ┃ routes.size == 0         ┃  ┃      ┃ routes.size == 0         ┃      ┃ routes.size == 0         ┃
//    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛  ┃      ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛      ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//               ^ ^                ┃                  ┃                              ┃      ^
//               ┃ ┃                ┃                  ┃                              v      ┃
//               ┃ ┃                ┃                  ┃                     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓
//               ┃ ┃                ┃                  ┗━━━━━━━━━━━━━━━━━━━ >┃ ActiveNavigation         ┃
//               ┃ ┃                ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ >┃--------------------------┃
//               ┃ ┃                                                         ┃ destination != null      ┃
//               ┃ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┃ previewRoute.size > 0    ┃
//               ┃                                                           ┃ routes.size > 0          ┃
//    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓                                           ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//    ┃ Arrival                  ┃                                                       ┃
//    ┃--------------------------┃                                         (onFinalDestinationArrival())
//    ┃ destination != null      ┃                                                       ┃
//    ┃ previewRoute.size > 0    ┃< ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//    ┃ routes.size > 0          ┃
//    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//
// ┏ ┓ ┗ ┛ ━ ┃ ┣ ╋ ┫
//
