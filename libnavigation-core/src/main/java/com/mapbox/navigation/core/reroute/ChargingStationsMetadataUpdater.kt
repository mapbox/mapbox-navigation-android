package com.mapbox.navigation.core.reroute

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.getWaypointMetadata
import com.mapbox.navigation.base.internal.route.getChargingStationsCurrentType
import com.mapbox.navigation.base.internal.route.getChargingStationsId
import com.mapbox.navigation.base.internal.route.getChargingStationsPower
import com.mapbox.navigation.base.internal.route.getWaypointMetadataOrEmpty
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.routeoptions.isEVRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// It's a temporary platform side workaround, the final solution will be on NN side.
// See https://mapbox.atlassian.net/browse/NN-854
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
        originalRoute.getServerProvidedChargingStationsIds()
    )
    // TODO: optimise java memory usage creating a copy of a response NAVAND-1437
    NavigationRoute.create(updatedResponse, rerouteRouteOptions, newPrimaryRoute.origin)
}

fun updateChargingStationsTypes(
    response: DirectionsResponse,
    serverProvidedStationsIds: Set<String>
): DirectionsResponse {
    val updatedRoutes = response.routes().map { route ->
        val updatedWaypoints =
            (route.waypoints() ?: emptyList()).map { waypoint ->
                val waypointUnrecognizedProperties = waypoint.unrecognizedJsonProperties
                val waypointMetadata = waypoint.getWaypointMetadata()
                val stationId = waypointMetadata?.getStationId()
                if (stationId != null && serverProvidedStationsIds.contains(stationId)) {
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
        route.toBuilder().waypoints(updatedWaypoints).build()
    }
    val updatedResponse = response.toBuilder().routes(updatedRoutes).build()
    return updatedResponse
}

private fun updateChargingStationsMetadataBasedOnRequestUrl(
    rerouteRouteOptions: RouteOptions,
    newPrimaryRoute: NavigationRoute
): DirectionsResponse {

    val response = newPrimaryRoute.directionsResponse
    val updatedResponse = response.toBuilder()
    if (rerouteRouteOptions.waypointsPerRoute() == true) {
        val updatedRoutes = response.routes().map { route ->
            val waypoints = route.waypoints() ?: emptyList()
            val updatedWaypoints = updateWaypoints(rerouteRouteOptions, waypoints)
            route.toBuilder().waypoints(updatedWaypoints).build()
        }
        updatedResponse.routes(updatedRoutes)
    } else {
        val waypoints = response.waypoints() ?: emptyList()
        val updatedWaypoints = updateWaypoints(rerouteRouteOptions, waypoints)
        updatedResponse.waypoints(updatedWaypoints)
    }
    return updatedResponse.build()
}

private fun updateWaypoints(
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
            if (waypoint.getWaypointMetadata() == null && updatedMetadata.jsonMetadata.size() == 0) {
                waypoint
            } else {
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

private fun NavigationRoute.getServerProvidedChargingStationsIds(): Set<String> =
    this.waypoints?.mapNotNull {
        val metadata = it.getWaypointMetadata()
        if (metadata?.isServerProvided() == true) {
            metadata.getStationId()
        } else {
            null
        }
    }?.toSet() ?: emptySet()
