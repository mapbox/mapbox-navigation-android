package com.mapbox.navigation.tripdata.shield.internal.api

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun MapboxRouteShieldApi.getRouteShieldsFromModels(
    shieldsToDownload: List<RouteShieldToDownload>,
): List<Expected<RouteShieldError, RouteShieldResult>> {
    return suspendCoroutine { continuation ->
        getRouteShieldsInternal(shieldsToDownload) {
            continuation.resume(it)
        }
    }
}
