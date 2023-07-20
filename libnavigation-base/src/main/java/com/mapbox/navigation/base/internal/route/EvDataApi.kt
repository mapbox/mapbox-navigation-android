package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions

fun DirectionsWaypoint.getChargingStationId(): String? =
    getWaypointMetadata()?.getStationId()

fun DirectionsWaypoint.getChargingStationType(): String? =
    getWaypointMetadata()?.getType()

fun DirectionsWaypoint.getWaypointMetadata(): ChargingStationMetadata? {
    val waypointUnrecognizedProperties = this.unrecognizedJsonProperties
    val waypointMetadata = waypointUnrecognizedProperties?.get("metadata")
        ?.asJsonObject
    return waypointMetadata?.let { ChargingStationMetadata(it) }
}

fun DirectionsWaypoint.getWaypointMetadataOrEmpty() = getWaypointMetadata()
    ?: ChargingStationMetadata.createEmpty()

@JvmInline
value class ChargingStationMetadata internal constructor(val jsonMetadata: JsonObject) {
    fun getType(): String? = jsonMetadata.get("type")?.asString
    fun getStationId(): String? = jsonMetadata.get("station_id")?.asString
    fun isServerProvided(): Boolean = getType() == "charging-station"

    fun deepCopy() = ChargingStationMetadata(jsonMetadata.deepCopy())

    fun setServerAddedTypeToUserProvided() {
        jsonMetadata.add("type", JsonPrimitive("charging-station"))
        jsonMetadata.add("was_requested_as_user_provided", JsonPrimitive(true))
    }

    fun setPowerKw(chargingStationsPowerKw: Int) {
        jsonMetadata.add("power_kw", JsonPrimitive(chargingStationsPowerKw))
    }

    fun setStationId(chargingStationId: String) {
        jsonMetadata.add("station_id", JsonPrimitive(chargingStationId))
    }

    fun setCurrentType(currentType: String) {
        jsonMetadata.add("current_type", JsonPrimitive(currentType))
    }

    companion object {
        fun createEmpty()  = ChargingStationMetadata(JsonObject())
    }
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
