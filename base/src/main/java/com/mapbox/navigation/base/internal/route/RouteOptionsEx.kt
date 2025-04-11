package com.mapbox.navigation.base.internal.route

import androidx.annotation.RestrictTo
import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.RouteOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
const val ROUTE_OPTIONS_KEY_ENGINE = "engine"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
const val ROUTE_OPTIONS_VALUE_ELECTRIC = "electric"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun RouteOptions.isEVRoute(): Boolean =
    unrecognizedJsonProperties.isEVRoute()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun Map<String, JsonElement>?.isEVRoute(): Boolean =
    this?.get(
        ROUTE_OPTIONS_KEY_ENGINE,
    )?.asStringOrNull() == ROUTE_OPTIONS_VALUE_ELECTRIC

private fun JsonElement.asStringOrNull(): String? = try {
    asString
} catch (ex: Throwable) {
    null
}
