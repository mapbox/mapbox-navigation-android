package com.mapbox.navigation.dropin.internal

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxNavigationViewApi(
    private val store: Store
) : NavigationViewApi() {

    override fun startFreeDrive() {
        store.dispatch(endNavigation())
    }

    override fun startDestinationPreview(point: Point) {
        store.dispatch(DestinationAction.SetDestination(Destination(point)))
        store.dispatch(NavigationStateAction.Update(NavigationState.DestinationPreview))
    }

    override fun startRoutePreview(): Expected<Throwable, Unit> =
        runCatchingExpected {
            checkDestination()
            checkPreviewRoutes()

            store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
        }

    override fun startRoutePreview(routes: List<NavigationRoute>): Expected<Throwable, Unit> =
        runCatchingExpected {
            require(routes.isNotEmpty()) { "routes cannot be empty" }
            val point = checkNotNull(routes.first().getDestination()) {
                "destination not found in a given route"
            }

            setDestination(point)
            setPreviewRoutes(routes)
            startRoutePreview()
        }

    override fun startNavigation(): Expected<Throwable, Unit> =
        runCatchingExpected {
            checkDestination()
            checkPreviewRoutes()

            val routes = (store.state.value.previewRoutes as RoutePreviewState.Ready).routes
            setRoutes(routes)
            store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }

    override fun startNavigation(routes: List<NavigationRoute>): Expected<Throwable, Unit> =
        runCatchingExpected {
            require(routes.isNotEmpty()) { "routes cannot be empty" }
            val point = checkNotNull(routes.first().getDestination()) {
                "destination not found in a given route"
            }

            setDestination(point)
            setPreviewRoutes(routes)
            startNavigation()
        }

    override fun startArrival(): Expected<Throwable, Unit> =
        runCatchingExpected {
            checkDestination()
            checkRoutes()

            store.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
        }

    override fun startArrival(routes: List<NavigationRoute>): Expected<Throwable, Unit> =
        runCatchingExpected {
            require(routes.isNotEmpty()) { "routes cannot be empty" }
            val point = checkNotNull(routes.first().getDestination()) {
                "destination not found in a given route"
            }

            setDestination(point)
            setPreviewRoutes(routes)
            setRoutes(routes)
            startArrival()
        }

    override fun isReplayEnabled(): Boolean {
        return store.state.value.tripSession.isReplayEnabled
    }

    override fun routeReplayEnabled(enabled: Boolean) {
        if (enabled) {
            store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
        } else {
            store.dispatch(TripSessionStarterAction.EnableTripSession)
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkDestination() {
        checkNotNull(store.state.value.destination) { "Destination not set." }
    }

    @Throws(IllegalStateException::class)
    private fun checkPreviewRoutes() {
        val previewState = store.state.value.previewRoutes
        check(
            previewState is RoutePreviewState.Ready && previewState.routes.isNotEmpty()
        ) {
            "Preview Routes not set."
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkRoutes() {
        check(store.state.value.routes.isNotEmpty()) { "Routes not set." }
    }

    private fun setDestination(point: Point) {
        store.dispatch(DestinationAction.SetDestination(Destination(point)))
    }

    private fun setPreviewRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutePreviewAction.Ready(routes))
    }

    private fun setRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutesAction.SetRoutes(routes))
    }

    private fun NavigationRoute.getDestination(): Point? {
        return directionsResponse.waypoints()?.lastOrNull()?.location()
    }
}

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it as a failure.
 */
private inline fun <R : Any> runCatchingExpected(block: () -> R): Expected<Throwable, R> {
    return try {
        ExpectedFactory.createValue(block())
    } catch (e: Throwable) {
        ExpectedFactory.createError(e)
    }
}
