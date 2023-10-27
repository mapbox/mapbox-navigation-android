package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.utils.RouteParsingManager
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ThreadController

internal object RouteAlternativesControllerProvider {

    fun create(
        options: RouteAlternativesOptions,
        navigator: MapboxNativeNavigator,
        tripSession: TripSession,
        threadController: ThreadController,
        routeParsingManager: RouteParsingManager
    ) = RouteAlternativesController(
        options,
        navigator,
        tripSession,
        threadController,
        routeParsingManager
    )
}
