package com.mapbox.navigation.core.reroute

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
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
    val newPrimaryRoute = newRoutes.first()
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
            (route.waypoints() ?: emptyList()).mapIndexed { waypointIndex, waypoint ->
                val waypointUnrecognizedProperties = waypoint.unrecognizedJsonProperties
                val waypointMetadata = waypointUnrecognizedProperties?.get("metadata")
                    ?.asJsonObject
                val stationId = waypointMetadata
                    ?.get("station_id")
                    ?.asString
                if (stationId != null && serverProvidedStationsIds.contains(stationId)) {
                    val updatedMetadata = waypointMetadata.deepCopy()
                    updatedMetadata.remove("type")
                    updatedMetadata.add("type", JsonPrimitive("charging-station"))
                    val updatedUnrecognizedProperties = waypointUnrecognizedProperties
                        .toMutableMap()
                    updatedUnrecognizedProperties["metadata"] = updatedMetadata
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
    val chargingStationsPowers = rerouteRouteOptions
        .getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_power")
    val chargingStationsIds = rerouteRouteOptions
        .getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_id")
    val chargingStationsCurrentTypes = rerouteRouteOptions
        .getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_current_type")
    val response = newPrimaryRoute.directionsResponse
    val updatedRoutes = response.routes().map { route ->
        val updatedWaypoints =
            (route.waypoints() ?: emptyList()).mapIndexed { waypointIndex, waypoint ->
                val chargingStationId = chargingStationsIds?.get(waypointIndex)
                val chargingStationsPower = chargingStationsPowers?.get(waypointIndex)
                    ?.toIntOrNull()
                    ?.let { it / 1000 }
                val chargingStationsCurrentType = chargingStationsCurrentTypes?.get(waypointIndex)
                waypoint.toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "metadata" to JsonObject().apply {
                                if (chargingStationsPower != null) {
                                    add("power_kw", JsonPrimitive(chargingStationsPower))
                                }
                                if (!chargingStationId.isNullOrEmpty()) {
                                    add("station_id", JsonPrimitive(chargingStationId))
                                }
                                if (!chargingStationsCurrentType.isNullOrEmpty()) {
                                    add(
                                        "current_type",
                                        JsonPrimitive(chargingStationsCurrentType)
                                    )
                                }
                            }
                        )
                    )
                    .build()
            }
        route.toBuilder().waypoints(updatedWaypoints).build()
    }
    val updatedResponse = response.toBuilder().routes(updatedRoutes).build()
    return updatedResponse
}

private fun RouteOptions.getUnrecognisedSemicolonSeparatedParameter(name: String) =
    unrecognizedJsonProperties
        ?.get(name)
        ?.asString
        ?.split(";")

private fun NavigationRoute.getServerProvidedChargingStationsIds(): Set<String> =
    this.waypoints?.mapNotNull {
        val metadata = it.unrecognizedJsonProperties?.get("metadata")?.asJsonObject
        if (metadata?.get("type")?.asString == "charging-station") {
            metadata.get("station_id")?.asString
        } else {
            null
        }
    }?.toSet() ?: emptySet()
