package com.mapbox.navigation.instrumentation_tests

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.ui.listeners.NavigationListener

fun routesRequestCallback(
    onRoutesReady: ((routes: List<DirectionsRoute>) -> Unit)? = null,
    onRoutesRequestFailure: ((throwable: Throwable, routeOptions: RouteOptions) -> Unit)? = null,
    onRoutesRequestCanceled: ((routeOptions: RouteOptions) -> Unit)? = null
) = object : RoutesRequestCallback {
    override fun onRoutesReady(routes: List<DirectionsRoute>) {
        onRoutesReady?.invoke(routes)
    }

    override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
        onRoutesRequestFailure?.invoke(throwable, routeOptions)
    }

    override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
        onRoutesRequestCanceled?.invoke(routeOptions)
    }
}

fun navigationListener(
    onNavigationFinished: (() -> Unit)? = null,
    onNavigationRunning: (() -> Unit)? = null,
    onCancelNavigation: (() -> Unit)? = null
) = object : NavigationListener {
    override fun onNavigationFinished() {
        onNavigationFinished?.invoke()
    }

    override fun onNavigationRunning() {
        onNavigationRunning?.invoke()
    }

    override fun onCancelNavigation() {
        onCancelNavigation?.invoke()
    }
}

fun arrivalObserver(
    onNextRouteLegStart: (() -> Unit)? = null,
    onFinalDestinationArrival: (() -> Unit)? = null
) = object : ArrivalObserver {
    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        onNextRouteLegStart?.invoke()
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        onFinalDestinationArrival?.invoke()
    }
}
