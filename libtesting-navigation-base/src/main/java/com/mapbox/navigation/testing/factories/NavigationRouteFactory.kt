package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import java.nio.charset.StandardCharsets
import java.util.UUID

fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute(),
    routeInfo: RouteInfo = RouteInfo(emptyList()),
    nativeWaypoints: List<Waypoint>? = null
): NavigationRoute {
    val response = createDirectionsResponse(
        routes = listOf(directionsRoute),
        uuid = directionsRoute.requestUuid()
    )
    return createNavigationRoutes(
        response = response,
        routesInfoMapper = { routeInfo },
        waypointsMapper = { waypoints, routeOptions ->
            nativeWaypoints ?: mapToNativeWaypoints(waypoints, routeOptions)
        },
    ).first()
}

fun createNavigationRoutes(
    response: DirectionsResponse = createDirectionsResponse(),
    options: RouteOptions = response.routes().first().routeOptions()!!,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard,
    routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    waypointsMapper: (List<DirectionsWaypoint>, RouteOptions?) -> List<Waypoint> = ::mapToNativeWaypoints
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
    private val waypointsMapper: (List<DirectionsWaypoint>, RouteOptions?) -> List<Waypoint> = ::mapToNativeWaypoints
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
            nativeWaypointsMapper = waypointsMapper
        )
        return ExpectedFactory.createValue(result)
    }

    override fun parseDirectionsResponse(
        response: DataRef,
        request: String,
        routerOrigin: RouterOrigin
    ): Expected<String, List<RouteInterface>> {
        response.buffer.position(0)
        val stringResponse = StandardCharsets.UTF_8.decode(response.buffer).toString();
        val result = createRouteInterfacesFromDirectionRequestResponse(
            requestUri = request,
            response = stringResponse,
            routerOrigin = routerOrigin,
            routesInfoMapper = routesInfoMapper,
            nativeWaypointsMapper = waypointsMapper
        )
        return ExpectedFactory.createValue(result)
    }
}

fun createRouteInterfacesFromDirectionRequestResponse(
    requestUri: String,
    response: String,
    routerOrigin: RouterOrigin = RouterOrigin.Offboard,
    routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    nativeWaypointsMapper: (List<DirectionsWaypoint>, RouteOptions?) -> List<Waypoint> =
        ::mapToNativeWaypoints
): List<RouteInterface> {
    val responseModel = DirectionsResponse.fromJson(response)
    return responseModel.routes()
        .map { directionsRoute ->
            createRouteInterface(
                responseUUID = directionsRoute.requestUuid() ?: "local@${UUID.randomUUID()}",
                routeIndex = directionsRoute.routeIndex()!!.toInt(),
                responseJson = response,
                routerOrigin = routerOrigin.mapToNativeRouteOrigin(),
                requestURI = requestUri,
                routeInfo = routesInfoMapper(directionsRoute),
                waypoints = nativeWaypointsMapper(
                    responseModel.waypoints() ?: directionsRoute.waypoints() ?: emptyList(),
                    directionsRoute.routeOptions()
                )
            )
        }
}

fun createRouteInfo() = RouteInfo(emptyList())

fun mapToNativeWaypoints(
    directionsWaypoints: List<DirectionsWaypoint>,
    routeOptions: RouteOptions?
): List<Waypoint> {
    return directionsWaypoints.mapIndexed { index: Int, directionsWaypoint ->
        createNativeWaypoint(
            directionsWaypoint.name(),
            directionsWaypoint.location(),
            directionsWaypoint.distance(),
            null,
            routeOptions?.waypointTargetsList()?.get(index),
            when {
                directionsWaypoint.getUnrecognizedProperty("metadata")
                    ?.asJsonObject?.get("type")
                    ?.asString == "charging-station" -> WaypointType.EV_CHARGING_SERVER
                directionsWaypoint.getUnrecognizedProperty("metadata")
                    ?.asJsonObject?.get("type")
                    ?.asString == "user-provided-charging-station" -> WaypointType.EV_CHARGING_USER
                routeOptions?.waypointIndicesList()
                    ?.contains(index)?.not() == true -> WaypointType.SILENT
                else -> WaypointType.REGULAR
            }
        )
    }
}
