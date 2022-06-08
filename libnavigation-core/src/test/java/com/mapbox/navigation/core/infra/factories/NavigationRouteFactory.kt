package com.mapbox.navigation.core.infra.factories

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createRouteInterface
import com.mapbox.navigator.RouteInterface

fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute()
): NavigationRoute {
    return com.mapbox.navigation.base.internal.route.createNavigationRoute(
        directionsRoute,
        object : SDKRouteParser {
            override fun parseDirectionsResponse(
                response: String,
                request: String,
                routerOrigin: RouterOrigin
            ): Expected<String, List<RouteInterface>> {
                return ExpectedFactory.createValue(
                    listOf(
                        createRouteInterface(
                            responseUUID = directionsRoute.requestUuid() ?: "null",
                            routeIndex = directionsRoute.routeIndex()!!.toInt(),
                            responseJson = response,
                            routerOrigin = routerOrigin.mapToNativeRouteOrigin(),
                            requestURI = directionsRoute.routeOptions()!!.toUrl("test")
                                .toString()
                        ),
                    )
                )
            }
        }
    )
}