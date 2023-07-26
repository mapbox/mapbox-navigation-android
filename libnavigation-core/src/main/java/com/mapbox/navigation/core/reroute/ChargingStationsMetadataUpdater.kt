package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.getChargingStationsCurrentType
import com.mapbox.navigation.base.internal.route.getChargingStationsId
import com.mapbox.navigation.base.internal.route.getChargingStationsPower
import com.mapbox.navigation.base.internal.route.getWaypointMetadata
import com.mapbox.navigation.base.internal.route.getWaypointMetadataOrEmpty
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.routeoptions.isEVRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun restoreChargingStationsMetadata(
    originalRoute: NavigationRoute,
    newRoutes: List<NavigationRoute>,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default
): List<NavigationRoute> = withContext(workerDispatcher) {
    val newPrimaryRoute = newRoutes.firstOrNull() ?: return@withContext newRoutes
    val rerouteRouteOptions = newPrimaryRoute.routeOptions
    if (!rerouteRouteOptions.isEVRoute()) {
        return@withContext newRoutes
    }
    val updatedResponseWithoutCorrectTypes = if (newPrimaryRoute.origin == RouterOrigin.Onboard) {
        updateChargingStationsMetadataBasedOnRequestUrl(rerouteRouteOptions, newPrimaryRoute)
    } else {
        newPrimaryRoute.directionsResponse
    }
    val updatedResponse = updateChargingStationsTypes(
        updatedResponseWithoutCorrectTypes,
        rerouteRouteOptions,
        originalRoute.getServerProvidedChargingStationsInfo()
    )
    // TODO: optimise java memory usage creating a copy of a response NAVAND-1437
    NavigationRoute.create(updatedResponse, rerouteRouteOptions, newPrimaryRoute.origin)
}

private fun updateChargingStationsTypes(
    response: DirectionsResponse,
    routeOptions: RouteOptions,
    serverProvidedStationsInfo: Map<String, ServerAddedChargingStationInfo>
): DirectionsResponse {
    return response.updateWaypoints(routeOptions) {
        updateChargingStationTypes(it, serverProvidedStationsInfo)
    }
}

private fun updateChargingStationTypes(
    waypoints: List<DirectionsWaypoint>,
    serverProvidedStationsInfo: Map<String, ServerAddedChargingStationInfo>
): List<DirectionsWaypoint> {
    val updatedWaypoints =
        waypoints.mapIndexed { index, waypoint ->
            val waypointUnrecognizedProperties = waypoint.unrecognizedJsonProperties
            val waypointMetadata = waypoint.getWaypointMetadata()
            val stationId = waypointMetadata?.getStationId()
            if (stationId != null && serverProvidedStationsInfo.contains(stationId) && serverProvidedStationsInfo[stationId]?.indexes?.contains(index) == true) {
                val updatedMetadata = waypointMetadata.deepCopy()
                updatedMetadata.setServerAddedTypeToUserProvided()
                val updatedUnrecognizedProperties = waypointUnrecognizedProperties
                    ?.toMutableMap() ?: mutableMapOf()
                updatedUnrecognizedProperties["metadata"] = updatedMetadata.jsonMetadata
                waypoint.toBuilder()
                    .unrecognizedJsonProperties(updatedUnrecognizedProperties)
                    .build()
            } else {
                waypoint
            }
        }
    return updatedWaypoints
}

// It's a temporary platform side workaround, the final solution will be on NN side.
// See https://mapbox.atlassian.net/browse/NN-854
private fun updateChargingStationsMetadataBasedOnRequestUrl(
    rerouteRouteOptions: RouteOptions,
    newPrimaryRoute: NavigationRoute
): DirectionsResponse {
    return newPrimaryRoute.directionsResponse.updateWaypoints(rerouteRouteOptions) {
        updateMetadataBasedOnRequestUrl(rerouteRouteOptions, it)
    }
}

private fun updateMetadataBasedOnRequestUrl(
    rerouteRouteOptions: RouteOptions,
    waypoints: List<DirectionsWaypoint>
): List<DirectionsWaypoint> {
    val chargingStationsPowers = rerouteRouteOptions.getChargingStationsPower()
    val chargingStationsIds = rerouteRouteOptions.getChargingStationsId()
    val chargingStationsCurrentTypes = rerouteRouteOptions.getChargingStationsCurrentType()
    val updatedWaypoints =
        (waypoints).mapIndexed { waypointIndex, waypoint ->
            val chargingStationId = chargingStationsIds?.get(waypointIndex)
            val chargingStationsPower = chargingStationsPowers?.get(waypointIndex)
                ?.let { it / 1000 }
            val chargingStationsCurrentType = chargingStationsCurrentTypes?.get(waypointIndex)
            val updatedMetadata = waypoint.getWaypointMetadataOrEmpty().deepCopy()
            if (chargingStationsPower != null) {
                updatedMetadata.setPowerKw(chargingStationsPower)
            }
            if (!chargingStationId.isNullOrEmpty()) {
                updatedMetadata.setStationId(chargingStationId)
            }
            if (!chargingStationsCurrentType.isNullOrEmpty()) {
                updatedMetadata.setCurrentType(chargingStationsCurrentType)
            }
            if (waypoint.getWaypointMetadata() == null &&
                updatedMetadata.jsonMetadata.size() == 0
            ) {
                waypoint
            } else {
                updatedMetadata.setUserProvidedType()
                waypoint.toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "metadata" to updatedMetadata.jsonMetadata
                        )
                    )
                    .build()
            }
        }
    return updatedWaypoints
}

private class ServerAddedChargingStationInfo(
    val indexes: MutableSet<Int> = mutableSetOf()
)

private fun NavigationRoute.getServerProvidedChargingStationsInfo(): Map<String, ServerAddedChargingStationInfo> {
    val result = mutableMapOf<String, ServerAddedChargingStationInfo>()
    this.waypoints?.forEachIndexed { index, waypoint ->
        val metadata = waypoint.getWaypointMetadata() ?: return@forEachIndexed
        val stationId = metadata.getStationId()
        if (metadata.isServerProvided() && stationId != null) {
            val stationInfo = result[stationId] ?: ServerAddedChargingStationInfo()
            result[stationId] = stationInfo.apply {
                indexes.add(index)
            }
        }
    }
    return result
}

private fun DirectionsResponse.updateWaypoints(
    routeOptions: RouteOptions,
    updateBlock: (List<DirectionsWaypoint>) -> List<DirectionsWaypoint>
): DirectionsResponse {
    val updatedResponse = toBuilder()
    if (routeOptions.waypointsPerRoute() == true) {
        val updatedRoutes = routes().map { route ->
            val waypoints = route.waypoints() ?: emptyList()
            val updatedWaypoints = updateBlock(waypoints)
            route.toBuilder().waypoints(updatedWaypoints).build()
        }
        updatedResponse.routes(updatedRoutes)
    } else {
        val waypoints = waypoints() ?: emptyList()
        val updatedWaypoints = updateBlock(waypoints)
        updatedResponse.waypoints(updatedWaypoints)
    }
    return updatedResponse.build()
}
