package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.routeoptions.MapboxRouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

internal object RouteAlternativesControllerProvider {

    fun create(
        options: RouteAlternativesOptions,
        navigator: MapboxNativeNavigator,
        directionsSession: DirectionsSession,
        tripSession: TripSession,
        routeOptionsUpdater: MapboxRouteOptionsUpdater
    ) = RouteAlternativesController(
        options,
        navigator,
        directionsSession,
        tripSession,
        routeOptionsUpdater
    )
}
