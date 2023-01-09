package com.mapbox.navigation.ui.app.internal.navigation

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.destination.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

//
// given:
//   navRoutes = MapboxNavigation.navigationRoutes
//   onArrivalSignal = MapboxNavigation.onFinalDestinationArrival()
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
//    ┃ navRoutes.size == 0      ┃  ┃      ┃ navRoutes.size == 0      ┃      ┃ navRoutes.size == 0      ┃
//    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛  ┃      ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛      ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//               ^ ^                ┃                  ┃                              ┃      ^
//               ┃ ┃                ┃                  ┃                              v      ┃
//               ┃ ┃                ┃                  ┃                     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓
//               ┃ ┃                ┃                  ┗━━━━━━━━━━━━━━━━━━━ >┃ ActiveNavigation         ┃
//               ┃ ┃                ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ >┃--------------------------┃
//               ┃ ┃                                                         ┃ destination != null      ┃
//               ┃ ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┃ previewRoute.size > 0    ┃
//               ┃                                                           ┃ navRoutes.size > 0       ┃
//    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓                                           ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//    ┃ Arrival                  ┃                                                       ┃
//    ┃--------------------------┃                                               (onArrivalSignal)
//    ┃ destination != null      ┃                                                       ┃
//    ┃ previewRoute.size > 0    ┃< ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//    ┃ navRoutes.size > 0       ┃
//    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛
//
//
internal class NavigationStateMachine(
    private val initialState: NavigationState,
    private val destinationFlow: StateFlow<Destination?>,
    private val previewRoutesFlow: StateFlow<List<NavigationRoute>>,
    private val navigationRoutesFlow: StateFlow<List<NavigationRoute>>,
    private val onArrivalSignal: Flow<Any>
) {
    fun navigationState(scope: CoroutineScope): StateFlow<NavigationState> {
        val flow = MutableStateFlow(initialState)

        scope.launch {
            onArrivalSignal.collect {
                flow.value = NavigationState.Arrival
            }
        }

        scope.launch {
            combine(
                destinationFlow,
                previewRoutesFlow,
                navigationRoutesFlow
            ) { destination, previewRoutes, navigationRoutes ->
                when {
                    navigationRoutes.isNotEmpty() -> NavigationState.ActiveNavigation
                    previewRoutes.isNotEmpty() -> NavigationState.RoutePreview
                    destination != null -> NavigationState.DestinationPreview
                    else -> NavigationState.FreeDrive
                }
            }.collect {
                flow.value = it
            }
        }

        return flow
    }
}
