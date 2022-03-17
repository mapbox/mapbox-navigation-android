package com.mapbox.navigation.base.internal

import com.mapbox.bindgen.Expected
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteParser

interface SDKRouteParser {
    fun parseDirectionsResponse(
        response: String,
        request: String
    ): Expected<String, List<RouteInterface>>
}

object NativeRouteParserWrapper : SDKRouteParser {
    override fun parseDirectionsResponse(
        response: String,
        request: String
    ): Expected<String, List<RouteInterface>> =
        RouteParser.parseDirectionsResponse(response, request)
}
