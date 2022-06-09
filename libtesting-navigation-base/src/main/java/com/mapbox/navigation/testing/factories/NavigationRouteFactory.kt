package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
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
                            requestURI = directionsRoute.routeOptions()!!.toUrl("pk.*test_token*")
                                .toString()
                        ),
                    )
                )
            }
        }
    )
}

fun createNavigationRoutes(
    response: DirectionsResponse = createDirectionsResponse(),
    options: RouteOptions = response.routes().first().routeOptions()!!,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard,
): List<NavigationRoute> {
    val parser = object : SDKRouteParser {
        override fun parseDirectionsResponse(
            response: String,
            request: String,
            routerOrigin: RouterOrigin
        ): Expected<String, List<RouteInterface>> {
            val directionsResponse = DirectionsResponse.fromJson(response)
            val result = mutableListOf<RouteInterface>()
            for (directionsRoute in directionsResponse.routes()) {
                val route = createRouteInterface(
                    responseUUID = directionsRoute.requestUuid() ?: "null",
                    routeIndex = directionsRoute.routeIndex()!!.toInt(),
                    responseJson = response,
                    routerOrigin = routerOrigin.mapToNativeRouteOrigin(),
                    requestURI = directionsRoute.routeOptions()!!.toUrl("pk.*test_token*")
                        .toString()
                )
                result.add(route)
            }
            return ExpectedFactory.createValue(result)
        }

    }
    return com.mapbox.navigation.base.internal.route.createNavigationRoutes(
        response,
        options,
        parser,
        routerOrigin
    )
}