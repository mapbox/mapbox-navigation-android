package com.mapbox.navigation.core.ev

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.routeoptions.KEY_ENGINE
import com.mapbox.navigation.core.routeoptions.VALUE_ELECTRIC
import com.mapbox.navigation.core.routeoptions.isEVRoute

internal class EVRefreshDataProvider(
    private val evDynamicDataHolder: EVDynamicDataHolder
) {

    fun get(initialRouteOptions: RouteOptions): Map<String, String> {
        val result = hashMapOf<String, String>()
        if (initialRouteOptions.isEVRoute()) {
            result[KEY_ENGINE] = VALUE_ELECTRIC
            result.putAll(
                evDynamicDataHolder.currentData(initialRouteOptions.unrecognizedJsonProperties!!)
            )
        }
        return result
    }
}
