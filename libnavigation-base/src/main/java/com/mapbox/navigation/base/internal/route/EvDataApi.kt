package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonObject
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions

fun DirectionsWaypoint.getChargingStationId(): String? =
    getChargingStationWaypointMetadata()?.getStationId()

fun DirectionsWaypoint.getChargingStationType(): String? =
    getChargingStationWaypointMetadata()?.getType()

fun DirectionsWaypoint.getChargingStationWaypointMetadata(): ChargingStationMetadata? {
    val waypointUnrecognizedProperties = this.unrecognizedJsonProperties
    val waypointMetadata = waypointUnrecognizedProperties?.get("metadata")
        ?.asJsonObject
    return waypointMetadata?.let { ChargingStationMetadata(it) }
}

@JvmInline
value class ChargingStationMetadata internal constructor(val metadata: JsonObject) {
    fun getType(): String? = metadata.get("type")?.asString
    fun getStationId(): String? = metadata.get("station_id")?.asString
    fun isServerProvided(): Boolean = getType() == "charging-station"

    fun copy() = ChargingStationMetadata(metadata.deepCopy())
}

fun RouteOptions.getChargingStationsPower(): List<Int?>? =
    getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_power")?.map {
        it.toIntOrNull()
    }

fun RouteOptions.getChargingStationsId(): List<String>? =
    getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_id")

fun RouteOptions.getChargingStationsCurrentType(): List<String>? =
    getUnrecognisedSemicolonSeparatedParameter("waypoints.charging_station_current_type")

private fun RouteOptions.getUnrecognisedSemicolonSeparatedParameter(name: String) =
    unrecognizedJsonProperties
        ?.get(name)
        ?.asString
        ?.split(";")
