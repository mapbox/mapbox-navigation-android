package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mapbox.api.directions.v5.models.DirectionsWaypoint

internal object WaypointsParser {

    fun parse(json: JsonElement?): List<DirectionsWaypoint?>? {
        if (json?.isJsonArray != true) {
            return null
        }
        val array = json.asJsonArray
        return array.map { waypointJson ->
            if (waypointJson.asJsonObjectOrElse { JsonObject() }.size() == 0) {
                null
            } else {
                try {
                    DirectionsWaypoint.fromJson(waypointJson.toString())
                } catch (ex: Throwable) {
                    null
                }
            }
        }
    }

    private fun JsonElement.asJsonObjectOrElse(defaultValueBlock: () -> JsonObject): JsonObject {
        return if (isJsonObject) asJsonObject else defaultValueBlock()
    }
}
