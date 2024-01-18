package com.mapbox.navigation.dropin.navigationview

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.getDestination
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.dropin.NavigationViewApiError
import com.mapbox.navigation.dropin.NavigationViewApiErrorTypes
import com.mapbox.navigation.dropin.camera.recenterCamera
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.app.internal.showDestinationPreview
import com.mapbox.navigation.ui.app.internal.showRoutePreview
import com.mapbox.navigation.ui.app.internal.startActiveNavigation
import com.mapbox.navigation.ui.app.internal.startArrival
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer

internal class MapboxNavigationViewApi(
    private val store: Store
) : NavigationViewApi() {

    override fun startFreeDrive() {
        store.dispatch(endNavigation())
    }

    override fun startDestinationPreview(point: Point) {
        store.dispatch(showDestinationPreview(point))
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
            checkRoutes(routes)
            val point = findDestinationPoint(routes)

            store.dispatch(showRoutePreview(point, routes))
        }

    override fun startActiveGuidance(): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            checkDestination()
            checkPreviewRoutes()

            val routes = (store.state.value.previewRoutes as RoutePreviewState.Ready).routes
            store.dispatch(startActiveNavigation(routes))
        }

    override fun startActiveGuidance(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit> =
        runCatchingError {
            checkRoutes(routes)
            val point = findDestinationPoint(routes)

            store.dispatch(startActiveNavigation(point, routes))
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
            checkRoutes(routes)
            val point = findDestinationPoint(routes)

            store.dispatch(startArrival(point, routes))
        }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun isReplayEnabled(): Boolean {
        return MapboxNavigationApp.current()?.isReplayEnabled() ?: false
    }

    override fun routeReplayEnabled(enabled: Boolean) {
        if (enabled) {
            store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
        } else {
            store.dispatch(TripSessionStarterAction.EnableTripSession)
        }
    }

    override fun getCurrentVoiceInstructionsPlayer(): MapboxVoiceInstructionsPlayer? {
        return MapboxAudioGuidance.getRegisteredInstance().getCurrentVoiceInstructionsPlayer()
    }

    override fun recenterCamera() {
        store.recenterCamera()
    }

    @Throws(NavigationViewApiError::class)
    private fun checkDestination() {
        if (store.state.value.destination == null) {
            throw MissingDestinationInfoError()
        }
    }

    @Throws(NavigationViewApiError::class)
    private fun checkPreviewRoutes() {
        val previewState = store.state.value.previewRoutes
        if (previewState !is RoutePreviewState.Ready || previewState.routes.isEmpty()) {
            throw MissingPreviewRoutesInfoError()
        }
    }

    @Throws(NavigationViewApiError::class)
    private fun checkRoutes() {
        if (store.state.value.routes.isEmpty()) throw MissingRoutesInfoError()
    }

    @Throws(NavigationViewApiError::class)
    private fun checkRoutes(routes: List<NavigationRoute>) {
        if (routes.isEmpty()) throw InvalidRoutesInfoError()
    }

    @Throws(NavigationViewApiError::class)
    private fun findDestinationPoint(routes: List<NavigationRoute>): Point {
        if (routes.isEmpty()) throw InvalidRoutesInfoError()

        return routes.first().getDestination() ?: throw IncompleteRoutesInfoError()
    }

    @Suppress("FunctionName")
    internal companion object {
        fun MissingDestinationInfoError() = NavigationViewApiError(
            NavigationViewApiErrorTypes.MissingDestinationInfo,
            "Destination cannot be empty."
        )

        fun MissingPreviewRoutesInfoError() = NavigationViewApiError(
            NavigationViewApiErrorTypes.MissingPreviewRoutesInfo,
            "Preview Routes cannot be empty."
        )

        fun MissingRoutesInfoError() = NavigationViewApiError(
            NavigationViewApiErrorTypes.MissingRoutesInfo,
            "Routes cannot be empty."
        )

        fun InvalidRoutesInfoError() = NavigationViewApiError(
            NavigationViewApiErrorTypes.InvalidRoutesInfo,
            "Routes cannot be empty."
        )

        fun IncompleteRoutesInfoError() = NavigationViewApiError(
            NavigationViewApiErrorTypes.IncompleteRoutesInfo,
            "Missing destination info in a given route."
        )
    }
}

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching any [NavigationViewApiError] that was thrown from the [block] function execution and encapsulating it as a failure.
 */
private inline fun <R : Any> runCatchingError(
    block: () -> R
): Expected<NavigationViewApiError, R> {
    return try {
        ExpectedFactory.createValue(block())
    } catch (e: NavigationViewApiError) {
        ExpectedFactory.createError(e)
    }
}
