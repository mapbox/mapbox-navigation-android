package com.mapbox.navigation.base.internal.route

import androidx.annotation.WorkerThread
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import kotlinx.coroutines.runBlocking

@WorkerThread
fun createNavigationRoutes(
    directionsResponseJson: String,
    routeRequestUrl: String,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
    routeIndexOverride: Int? = null,
) = runBlocking {
    setupParsing(nativeRoute = false).parseDirectionsResponse(
        ResponseToParse(
            responseBody = directionsResponseJson.toDataRefJava(),
            routeRequest = routeRequestUrl,
            routerOrigin = routerOrigin,
            routeIndexOverride = routeIndexOverride,
        ),
    ).getOrThrow().routes
}
