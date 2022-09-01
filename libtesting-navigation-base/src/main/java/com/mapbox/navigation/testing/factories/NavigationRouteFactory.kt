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
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteInterface

fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute(),
    routeInfo: RouteInfo = RouteInfo(emptyList())
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
                                .toString(),
                            routeInfo = routeInfo
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
            val result = createRouteInterfacesFromDirectionRequestResponse(
                requestUri = request,
                response = response,
                routerOrigin = routerOrigin
            )
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

fun createRouteInterfacesFromDirectionRequestResponse(
    requestUri: String,
    response: String,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard
): List<RouteInterface> {
    return DirectionsResponse.fromJson(response).routes()
        .map { directionsRoute ->
            createRouteInterface(
                responseUUID = directionsRoute.requestUuid() ?: "null",
                routeIndex = directionsRoute.routeIndex()!!.toInt(),
                responseJson = response,
                routerOrigin = routerOrigin.mapToNativeRouteOrigin(),
                requestURI = requestUri
            )
        }
}
