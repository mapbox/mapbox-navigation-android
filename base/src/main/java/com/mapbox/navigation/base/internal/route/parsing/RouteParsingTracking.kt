package com.mapbox.navigation.base.internal.route.parsing

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.internal.route.RoutesResponse

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface RouteParsingTracking {
    fun routeResponseIsParsed(metadata: RoutesResponse.Metadata)
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun noTracking(): RouteParsingTracking = object : RouteParsingTracking {
    override fun routeResponseIsParsed(metadata: RoutesResponse.Metadata) {
    }
}
