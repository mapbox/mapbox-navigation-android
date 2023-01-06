package com.mapbox.navigation.ui.app.internal

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.ThunkAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction

/**
 * End Navigation ThunkAction creator.
 */
fun endNavigation() = ThunkAction { store ->
    store.setRoutes(emptyList())
    store.setPreviewRoutes(emptyList())
    store.setDestination(null)
    store.setNavigationState(NavigationState.FreeDrive)
}

/**
 * Show Destination Preview ThunkAction creator.
 */
fun showDestinationPreview(point: Point) = ThunkAction { store ->
    store.setDestination(point)
    store.setNavigationState(NavigationState.DestinationPreview)
}

/**
 * Show Route Preview ThunkAction creator.
 */
fun showRoutePreview(point: Point, routes: List<NavigationRoute>) = ThunkAction { store ->
    store.setDestination(point)
    store.setPreviewRoutes(routes)
    store.setNavigationState(NavigationState.RoutePreview)
}

/**
 * Start Active Navigation ThunkAction creator.
 */
fun startActiveNavigation(routes: List<NavigationRoute>) = ThunkAction { store ->
    store.setRoutes(routes)
    store.setNavigationState(NavigationState.ActiveNavigation)
}

/**
 * Start Active Navigation ThunkAction creator.
 */
fun startActiveNavigation(point: Point, routes: List<NavigationRoute>) = ThunkAction { store ->
    store.setDestination(point)
    store.setPreviewRoutes(routes)
    store.setRoutes(routes)
    store.setNavigationState(NavigationState.ActiveNavigation)
}

/**
 * Start Arrival ThunkAction creator.
 */
fun startArrival(point: Point, routes: List<NavigationRoute>) = ThunkAction { store ->
    store.setDestination(point)
    store.setPreviewRoutes(routes)
    store.setRoutes(routes)
    store.setNavigationState(NavigationState.Arrival)
}

private fun Store.setDestination(point: Point?) {
    val destination = point?.let { Destination(point) }
    dispatch(DestinationAction.SetDestination(destination))
}

private fun Store.setPreviewRoutes(routes: List<NavigationRoute>) {
    dispatch(RoutePreviewAction.Ready(routes))
}

private fun Store.setRoutes(routes: List<NavigationRoute>) {
    dispatch(RoutesAction.SetRoutes(routes))
}

private fun Store.setNavigationState(navState: NavigationState) {
    //dispatch(NavigationStateAction.Update(navState))
}
