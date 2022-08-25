package com.mapbox.navigation.dropin.internal

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.dropin.NavigationViewApiError
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

    override fun startRoutePreview(): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            checkDestination()
            checkPreviewRoutes()

            store.dispatch(NavigationStateAction.Update(NavigationState.RoutePreview))
        }

    override fun startRoutePreview(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            val point = findDestinationPoint(routes)

            setDestination(point)
            setPreviewRoutes(routes)
            startRoutePreview()
        }

    override fun startActiveGuidance(): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            checkDestination()
            checkPreviewRoutes()

            val routes = (store.state.value.previewRoutes as RoutePreviewState.Ready).routes
            setRoutes(routes)
            store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }

    override fun startActiveGuidance(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            val point = findDestinationPoint(routes)

            setDestination(point)
            setPreviewRoutes(routes)
            startActiveGuidance()
        }

    override fun startArrival(): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            checkDestination()
            checkRoutes()

            store.dispatch(NavigationStateAction.Update(NavigationState.Arrival))
        }

    override fun startArrival(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            val point = findDestinationPoint(routes)

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

    @Throws(NavigationViewApiError.MissingDestinationInfo::class)
    private fun checkDestination() {
        if (store.state.value.destination == null) {
            throw NavigationViewApiError.MissingDestinationInfo
        }
    }

    @Throws(NavigationViewApiError.MissingPreviewRoutesInfo::class)
    private fun checkPreviewRoutes() {
        val previewState = store.state.value.previewRoutes
        if (previewState !is RoutePreviewState.Ready || previewState.routes.isEmpty()) {
            throw NavigationViewApiError.MissingPreviewRoutesInfo
        }
    }

    @Throws(NavigationViewApiError.MissingRoutesInfo::class)
    private fun checkRoutes() {
        if (store.state.value.routes.isEmpty()) throw NavigationViewApiError.MissingRoutesInfo
    }

    @Throws(
        NavigationViewApiError.InvalidRoutesInfo::class,
        NavigationViewApiError.IncompleteRoutesInfo::class
    )
    private fun findDestinationPoint(routes: List<NavigationRoute>): Point {
        if (routes.isEmpty()) throw NavigationViewApiError.InvalidRoutesInfo

        return routes.first().getDestination() ?: throw NavigationViewApiError.IncompleteRoutesInfo
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
 * catching any [NavigationViewApiError] that was thrown from the [block] function execution and encapsulating it as a failure.
 */
@ExperimentalPreviewMapboxNavigationAPI
private inline fun <R : Any> runCatchingError(
    block: () -> R
): Expected<NavigationViewApiError, R> {
    return try {
        ExpectedFactory.createValue(block())
    } catch (e: NavigationViewApiError) {
        ExpectedFactory.createError(e)
    }
}
