package com.mapbox.navigation.dropin.internal

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.app.internal.endNavigation as endNavigationAction

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxNavigationViewApi(
    private val store: Store
) : NavigationViewApi() {

    override fun fetchRoutes(options: RouteOptions) {
        store.dispatch(RoutePreviewAction.FetchOptions(options))
    }

    override fun fetchRoutes(points: List<Point>) {
        store.dispatch(RoutePreviewAction.FetchPoints(points))
    }

    override fun setPreviewRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutePreviewAction.Ready(routes))
    }

    override fun setRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutesAction.SetRoutes(routes))
    }

    override fun setDestination(point: Point) {
        store.dispatch(DestinationAction.SetDestination(Destination(point)))
    }

    override fun enableTripSession() {
        store.dispatch(TripSessionStarterAction.EnableTripSession)
    }

    override fun enableReplaySession() {
        store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
    }

    override fun isReplayEnabled(): Boolean {
        return store.state.value.tripSession.isReplayEnabled
    }

    override fun startFreeDrive() {
        store.dispatch(RoutesAction.SetRoutes(emptyList()))
        store.dispatch(RoutePreviewAction.Ready(emptyList()))
        store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
    }

    @Throws(IllegalStateException::class)
    override fun startRoutePreview() {
        checkDestination()
        checkPreviewRoutes()

        store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
    }

    @Throws(IllegalStateException::class)
    override fun startNavigation() {
        checkDestination()
        checkPreviewRoutes()

        val routes = (store.state.value.previewRoutes as RoutePreviewState.Ready).routes
        store.dispatch(RoutesAction.SetRoutes(routes))
        store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
    }

    override fun startArrival() {
        store.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
    }

    override fun endNavigation() {
        store.dispatch(endNavigationAction())
    }

    @Throws(IllegalStateException::class)
    private fun checkDestination() {
        checkNotNull(store.state.value.destination) {
            "Destination not set. Use setDestination(point) to set Destination."
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkPreviewRoutes() {
        val previewState = store.state.value.previewRoutes
        check(
            previewState is RoutePreviewState.Ready && previewState.routes.isNotEmpty()
        ) {
            "Preview Routes not set. Use setPreviewRoutes(routes) to set Preview Routes."
        }
    }
}
