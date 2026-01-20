@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.noTracking
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.utils.internal.Time
import kotlinx.coroutines.CoroutineDispatcher

@Deprecated("Use RoutesParser instead")
suspend fun parseDirectionsResponse(
    dispatcher: CoroutineDispatcher,
    responseJson: DataRef,
    requestUrl: String,
    @RouterOrigin routerOrigin: String,
    nativeRoute: Boolean,
): Result<DirectionsResponseParsingSuccessfulResult> {
    val parsing = setupParsing(
        nativeRoute,
        Time.SystemClockImpl,
        noTracking(),
        dispatcher,
        { null },
    )
    return parsing.parseDirectionsResponse(
        DirectionsResponseToParse(
            responseJson,
            requestUrl,
            routerOrigin = routerOrigin,
            responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
        ),
    )
}
