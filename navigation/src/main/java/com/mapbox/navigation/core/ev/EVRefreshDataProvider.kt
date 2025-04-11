package com.mapbox.navigation.core.ev

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.ROUTE_OPTIONS_KEY_ENGINE
import com.mapbox.navigation.base.internal.route.ROUTE_OPTIONS_VALUE_ELECTRIC
import com.mapbox.navigation.base.internal.route.isEVRoute

internal class EVRefreshDataProvider(
    private val evDynamicDataHolder: EVDynamicDataHolder,
) {

    fun get(initialRouteOptions: RouteOptions): Map<String, String> {
        val result = hashMapOf<String, String>()
        if (initialRouteOptions.isEVRoute()) {
            result[ROUTE_OPTIONS_KEY_ENGINE] = ROUTE_OPTIONS_VALUE_ELECTRIC
            result.putAll(
                evDynamicDataHolder.currentData(initialRouteOptions.unrecognizedJsonProperties!!),
            )
        }
        return result
    }
}
