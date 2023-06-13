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
import com.mapbox.navigator.Waypoint

fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute(),
    routeInfo: RouteInfo = RouteInfo(emptyList()),
    waypoints: List<Waypoint> = createWaypoints(),
    routerOrigin: RouterOrigin = RouterOrigin.Offboard
): NavigationRoute {
    return createNavigationRoutes(
        response = createDirectionsResponse(
            routes = listOf(directionsRoute),
            uuid = directionsRoute.requestUuid()
        ),
        routesInfoMapper = { routeInfo },
        waypointsMapper = { waypoints },
        routerOrigin = routerOrigin
    ).first()
}

fun createNavigationRoutes(
    response: DirectionsResponse = createDirectionsResponse(),
    options: RouteOptions = response.routes().first().routeOptions()!!,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard,
    routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    waypointsMapper: (DirectionsRoute) -> List<Waypoint> = { createWaypoints() }
): List<NavigationRoute> {
    val parser = TestSDKRouteParser(
        routesInfoMapper = routesInfoMapper,
        waypointsMapper = waypointsMapper
    )
    return com.mapbox.navigation.base.internal.route.createNavigationRoutes(
        response,
        options,
        parser,
        routerOrigin
    )
}

class TestSDKRouteParser(
    private val routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    private val waypointsMapper: (DirectionsRoute) -> List<Waypoint> = { createWaypoints() }
) : SDKRouteParser {
    override fun parseDirectionsResponse(
        response: String,
        request: String,
        routerOrigin: RouterOrigin
    ): Expected<String, List<RouteInterface>> {
        val result = createRouteInterfacesFromDirectionRequestResponse(
            requestUri = request,
            response = response,
            routerOrigin = routerOrigin,
            routesInfoMapper = routesInfoMapper,
            waypointsMapper = waypointsMapper
        )
        return ExpectedFactory.createValue(result)
    }
}

fun createRouteInterfacesFromDirectionRequestResponse(
    requestUri: String,
    response: String,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard,
    routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    waypointsMapper: (DirectionsRoute) -> List<Waypoint> = { createWaypoints() }
): List<RouteInterface> {
    return DirectionsResponse.fromJson(response).routes()
        .map { directionsRoute ->
            createRouteInterface(
                responseUUID = directionsRoute.requestUuid() ?: "null",
                routeIndex = directionsRoute.routeIndex()!!.toInt(),
                responseJson = response,
                routerOrigin = routerOrigin.mapToNativeRouteOrigin(),
                requestURI = requestUri,
                routeInfo = routesInfoMapper(directionsRoute),
                waypoints = waypointsMapper(directionsRoute)
            )
        }
}

fun createRouteInfo() = RouteInfo(emptyList())