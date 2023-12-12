package com.mapbox.navigation.route.internal.util

import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory

internal fun parseDirectionsRouteRefresh(
    json: String,
): Expected<Throwable, DirectionsRouteRefresh> {
    return try {
        val route = DirectionsRefreshResponse.fromJson(json).route()
        if (route != null) {
            ExpectedFactory.createValue(route)
        } else {
            ExpectedFactory.createError(IllegalStateException("no route refresh returned"))
        }
    } catch (ex: Throwable) {
        ExpectedFactory.createError(ex)
    }
}
