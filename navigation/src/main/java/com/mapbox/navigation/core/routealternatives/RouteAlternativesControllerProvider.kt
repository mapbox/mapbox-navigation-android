package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.route.parsing.RouteInterfacesParser
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
        routeInterfacesParser: RouteInterfacesParser,
    ) = PerformanceTracker.trackPerformanceSync("RouteAlternativesControllerProvider#create") {
        RouteAlternativesController(
            options,
            navigator,
            tripSession,
            threadController,
            routeInterfacesParser,
        )
    }
}
