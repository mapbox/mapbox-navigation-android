package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonObject
import com.mapbox.api.directions.v5.models.DirectionsWaypoint

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