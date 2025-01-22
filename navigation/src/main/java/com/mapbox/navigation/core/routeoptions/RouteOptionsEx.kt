package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.RouteOptions

internal const val ROUTE_OPTIONS_KEY_ENGINE = "engine"
internal const val ROUTE_OPTIONS_VALUE_ELECTRIC = "electric"

internal fun RouteOptions.isEVRoute(): Boolean =
    unrecognizedJsonProperties.isEVRoute()

internal fun Map<String, JsonElement>?.isEVRoute(): Boolean =
    this?.get(
        ROUTE_OPTIONS_KEY_ENGINE,
    )?.asStringOrNull() == ROUTE_OPTIONS_VALUE_ELECTRIC

private fun JsonElement.asStringOrNull(): String? = try {
    asString
} catch (ex: Throwable) {
    null
}
