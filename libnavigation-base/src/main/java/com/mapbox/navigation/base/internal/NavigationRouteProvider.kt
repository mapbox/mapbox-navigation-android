package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouteInterface
import java.net.URL

object NavigationRouteProvider {

    fun createSingleRoute(nativeRoute: RouteInterface): NavigationRoute? {
        return try {
            NavigationRoute(
                directionsResponse = DirectionsResponse.fromJson(nativeRoute.responseJson),
                routeIndex = nativeRoute.routeIndex,
                routeOptions = RouteOptions.fromUrl(URL(nativeRoute.requestUri)),
                nativeRoute = nativeRoute
            )
        } catch (ex: Throwable) {
            null
        }
    }
}
