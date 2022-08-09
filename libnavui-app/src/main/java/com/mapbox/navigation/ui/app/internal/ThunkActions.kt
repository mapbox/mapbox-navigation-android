package com.mapbox.navigation.ui.app.internal

import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.ThunkAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/**
 * End Navigation ThunkAction creator.
 */
fun endNavigation() = ThunkAction { store ->
    store.dispatch(RoutesAction.SetRoutes(emptyList()))
    store.dispatch(RoutePreviewAction.Ready(emptyList()))
    store.dispatch(DestinationAction.SetDestination(null))
    store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
}

/**
 * Show Route Preview ThunkAction creator.
 */
fun CoroutineScope.showRoutePreview() = fetchRouteAndContinue { store ->
    store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
}

/**
 * Start Active Navigation ThunkAction creator.
 */
fun CoroutineScope.startActiveNavigation() = fetchRouteAndContinue { store ->
    if (store.state.value.previewRoutes is RoutePreviewState.Ready) {
        store.dispatch(
            RoutesAction.SetRoutes(
                (store.state.value.previewRoutes as RoutePreviewState.Ready).routes
            )
        )
        store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }
}

private fun CoroutineScope.fetchRouteAndContinue(continuation: (Store) -> Unit) =
    ThunkAction { store ->
        launch {
            if (fetchRouteIfNeeded(store)) {
                continuation(store)
            }
        }
    }

/**
 * Dispatch FetchPoints action and wait for RoutePreviewState.Ready.
 * Method returns immediately if already in RoutePreviewState.Ready or RoutePreviewState.Fetching, or if
 * required location or destination data is missing.
 *
 * @return `true` once in RoutePreviewState.Ready state, otherwise `false`
 */
private suspend fun fetchRouteIfNeeded(store: Store): Boolean {
    val storeState = store.state.value
    if (storeState.previewRoutes is RoutePreviewState.Ready) return true
    if (storeState.previewRoutes is RoutePreviewState.Fetching) return false

    return ifNonNull(
        storeState.location?.enhancedLocation?.toPoint(),
        storeState.destination
    ) { lastPoint, destination ->
        store.dispatch(RoutePreviewAction.FetchPoints(listOf(lastPoint, destination.point)))
        store.waitWhileFetching()
        store.state.value.previewRoutes is RoutePreviewState.Ready
    } ?: false
}

private suspend fun Store.waitWhileFetching() {
    select { it.previewRoutes }.takeWhile { it is RoutePreviewState.Fetching }.collect()
}
