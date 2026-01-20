@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.parsing

import androidx.annotation.RestrictTo
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class DirectionsResponseToParse(
    val responseBody: DataRef,
    val routeRequest: String,
    @RouterOrigin val routerOrigin: String,
    @ResponseOriginAPI val responseOriginAPI: String = ResponseOriginAPI.DIRECTIONS_API,
) {
    companion object {

        fun from(
            responseBody: DataRef,
            routeRequest: String,
            @RouterOrigin routerOrigin: String,
            @ResponseOriginAPI responseOriginAPI: String = ResponseOriginAPI.DIRECTIONS_API,
        ) = DirectionsResponseToParse(
            responseBody,
            routeRequest,
            routerOrigin,
            responseOriginAPI,
        )
    }
}
