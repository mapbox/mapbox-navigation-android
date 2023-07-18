package com.mapbox.navigation.core.reroute

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.routeoptions.isEVRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun preserveChargingStationsFromOldRoutes(
    originalRoute: NavigationRoute,
    newRoutes: List<NavigationRoute>
): List<NavigationRoute> = withContext(Dispatchers.Default) {
    if (!originalRoute.routeOptions.isEVRoute()) {
        return@withContext newRoutes
    }
    if (newRoutes.first().origin == RouterOrigin.Offboard) {
        return@withContext newRoutes
    }
    val newPrimaryRoute = newRoutes.first()
    val rerouteRouteOptions = newPrimaryRoute.routeOptions
    val chargingStationsIds = rerouteRouteOptions.getUnrecognisedParameter("waypoints.charging_station_id")
        ?: return@withContext newRoutes
    val chargingStationsPowers = rerouteRouteOptions.getUnrecognisedParameter("waypoints.charging_station_power")
        ?: return@withContext newRoutes
    val chargingStationsCurrentTypes = rerouteRouteOptions.getUnrecognisedParameter("waypoints.charging_station_current_type")
        ?: return@withContext newRoutes
    val response = newPrimaryRoute.directionsResponse
    val updatedRoutes = response.routes().map { route ->
        val updatedWaypoints = (route.waypoints() ?: emptyList()).mapIndexed { waypointIndex, waypoint ->
            val chargingStationId = chargingStationsIds[waypointIndex]
            val chargingStationsPower = chargingStationsPowers[waypointIndex]
                .toIntOrNull()
                ?.let { it / 1000 }
            val chargingStationsCurrentType = chargingStationsCurrentTypes[waypointIndex]
            if (chargingStationId != "" && chargingStationsPower != null && chargingStationsCurrentType != "") {
                waypoint.toBuilder()
                    .unrecognizedJsonProperties(
                        mapOf(
                            "metadata" to JsonObject().apply {
                                add("station_id", JsonPrimitive(chargingStationId))
                                add("power_kw", JsonPrimitive(chargingStationsPower))
                                add("current_type", JsonPrimitive(chargingStationsCurrentType))
                            }
                        )
                    )
                    .build()
            } else {
                waypoint
            }
        }
        route.toBuilder().waypoints(updatedWaypoints).build()
    }
    val updatedResponse = response.toBuilder().routes(updatedRoutes).build()
    NavigationRoute.create(updatedResponse, rerouteRouteOptions, RouterOrigin.Onboard)
}

private fun RouteOptions.getUnrecognisedParameter(name: String) =
    unrecognizedJsonProperties
        ?.get(name)
        ?.asString
        ?.split(";")