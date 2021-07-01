package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouteVariants
import com.mapbox.navigation.base.route.RouteWrapper

object RouteWrapperFactory {
    fun buildRouteWrapper(
        routes: List<DirectionsRoute>,
        @RouteVariants.RouterOrigin origin: String
    ): RouteWrapper = RouteWrapper(routes, origin)
}
