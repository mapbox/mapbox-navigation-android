package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.RouteOptions

const val KEY_ENGINE = "engine"
const val VALUE_ELECTRIC = "electric"

fun RouteOptions.isEVRoute(): Boolean =
    unrecognizedJsonProperties?.get(KEY_ENGINE)?.asStringOrNull() == VALUE_ELECTRIC

private fun JsonElement.asStringOrNull(): String? = try {
    asString
} catch (ex: Throwable) {
    null
}
