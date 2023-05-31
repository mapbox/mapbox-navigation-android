package com.mapbox.navigation.base.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteParser

interface SDKRouteParser {
    fun parseDirectionsResponse(
        response: String,
        request: String,
        routerOrigin: RouterOrigin,
    ): Expected<String, List<RouteInterface>>

    fun parseDirectionsResponse(
        response: DataRef,
        request: String,
        routerOrigin: RouterOrigin,
    ): Expected<String, List<RouteInterface>>

    companion object {
        var default: SDKRouteParser = NativeRouteParserWrapper()
            @VisibleForTesting set

        @VisibleForTesting
        fun resetDefault() {
            default = NativeRouteParserWrapper()
        }
    }
}

private class NativeRouteParserWrapper : SDKRouteParser {
    override fun parseDirectionsResponse(
        response: String,
        request: String,
        routerOrigin: RouterOrigin,
    ): Expected<String, List<RouteInterface>> =
        RouteParser.parseDirectionsResponse(
            response,
            request,
            routerOrigin.mapToNativeRouteOrigin()
        )

    override fun parseDirectionsResponse(
        response: DataRef,
        request: String,
        routerOrigin: RouterOrigin
    ): Expected<String, List<RouteInterface>> {
        return RouteParser.parseDirectionsResponse(
            response,
            request,
            routerOrigin.mapToNativeRouteOrigin()
        )
    }
}
