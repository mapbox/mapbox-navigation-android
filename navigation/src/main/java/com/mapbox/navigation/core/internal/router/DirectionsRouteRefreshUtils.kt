package com.mapbox.navigation.core.internal.router

import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.utils.toByteArray

internal fun parseDirectionsRouteRefresh(
    dataRef: DataRef,
): Expected<Throwable, DirectionsRouteRefresh> {
    return try {
        /**
         * TODO support DirectionsRefreshResponse creation from DataRef.
         * See DirectionsResponse.fromJson(reader)
         */
        val route =
            DirectionsRefreshResponse.fromJson(dataRef.toByteArray().decodeToString()).route()
        if (route != null) {
            ExpectedFactory.createValue(route)
        } else {
            ExpectedFactory.createError(IllegalStateException("no route refresh returned"))
        }
    } catch (ex: Throwable) {
        ExpectedFactory.createError(ex)
    }
}
