package com.mapbox.navigation.core.reroute

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.routeoptions.isEVRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// It's a temporary platform side workaround, the final solution will be on NN side.
// See https://mapbox.atlassian.net/browse/NN-854
internal suspend fun restoreChargingStationsMetadataFromUrl(
    newRoutes: List<NavigationRoute>,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default
): List<NavigationRoute> = withContext(workerDispatcher) {
    val newPrimaryRoute = newRoutes.first()
    val rerouteRouteOptions = newPrimaryRoute.routeOptions
    if (!rerouteRouteOptions.isEVRoute()) {
        return@withContext newRoutes
    }
    if (newPrimaryRoute.origin == RouterOrigin.Offboard) {
        return@withContext newRoutes
    }
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
    // TODO: optimise java memory usage creating a copy of a response NAVAND-1437
    NavigationRoute.create(updatedResponse, rerouteRouteOptions, RouterOrigin.Onboard)
}

private fun RouteOptions.getUnrecognisedSemicolonSeparatedParameter(name: String) =
    unrecognizedJsonProperties
        ?.get(name)
        ?.asString
        ?.split(";")
