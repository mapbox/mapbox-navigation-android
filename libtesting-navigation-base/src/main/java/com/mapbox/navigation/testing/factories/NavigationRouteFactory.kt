@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.testing.factories

import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.SDKRouteParser
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import java.nio.charset.StandardCharsets
import java.util.UUID

@JvmOverloads
fun createNavigationRoute(
    directionsRoute: DirectionsRoute = createDirectionsRoute(),
    routeInfo: RouteInfo = RouteInfo(emptyList()),
    nativeWaypoints: List<Waypoint>? = null,
    @RouterOrigin routerOrigin: String = RouterOrigin.ONLINE,
    responseWaypoints: List<DirectionsWaypoint> = listOf(createWaypoint(), createWaypoint())
): NavigationRoute {
    val response = createDirectionsResponse(
        routes = listOf(directionsRoute),
        uuid = directionsRoute.requestUuid(),
        routeOptions = directionsRoute.routeOptions(),
        responseWaypoints = responseWaypoints
    )
    return createNavigationRoutes(
        response = response,
        routesInfoMapper = { routeInfo },
        waypointsMapper = { waypoints, routeOptions ->
            nativeWaypoints ?: mapToNativeWaypoints(waypoints, routeOptions)
        },
        routerOrigin = routerOrigin
    ).first()
}

fun createNavigationRoutes(
    response: DirectionsResponse = createDirectionsResponse(),
    options: RouteOptions = response.routes().first().routeOptions()!!,
    @RouterOrigin routerOrigin: String = RouterOrigin.ONLINE,
    responseTimeElapsedSeconds: Long? = null,
    routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    waypointsMapper: (List<DirectionsWaypoint>, RouteOptions?) -> List<Waypoint> = ::mapToNativeWaypoints,
    @ResponseOriginAPI responseOriginAPI: String = DIRECTIONS_API,
    ): List<NavigationRoute> {
    val parser = TestSDKRouteParser(
        routesInfoMapper = routesInfoMapper,
        waypointsMapper = waypointsMapper
    )
    return com.mapbox.navigation.base.internal.route.testing.createNavigationRouteForTest(
        response,
        options,
        parser,
        routerOrigin,
        responseTimeElapsedSeconds,
        responseOriginAPI,
    )
}

class TestSDKRouteParser(
    private val routesInfoMapper: (DirectionsRoute) -> RouteInfo = { createRouteInfo() },
    private val waypointsMapper: (List<DirectionsWaypoint>, RouteOptions?) -> List<Waypoint> = ::mapToNativeWaypoints
) : SDKRouteParser {
    override fun parseDirectionsResponse(
        response: String,
        request: String,
        @RouterOrigin routerOrigin: String
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
        @RouterOrigin routerOrigin: String
    ): Expected<String, List<RouteInterface>> {
        val buffer = response.buffer.asReadOnlyBuffer()
        buffer.position(0)
        val stringResponse = StandardCharsets.UTF_8.decode(buffer).toString()
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
    @RouterOrigin routerOrigin: String = RouterOrigin.ONLINE,
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
                ),
                routeGeometry = directionsRoute.completeGeometryToPoints(),
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
            directionsWaypoint.getUnrecognizedProperty("metadata")?.toString(),
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

fun createBearing(
    angle: Double = 20.0,
    degrees: Double = 45.0
) = Bearing.builder()
    .angle(angle)
    .degrees(degrees)
    .build()
