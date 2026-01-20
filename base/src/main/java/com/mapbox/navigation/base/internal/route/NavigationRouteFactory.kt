package com.mapbox.navigation.base.internal.route

import androidx.annotation.WorkerThread
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.DirectionsResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.base.route.MapMatchingMatch
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.runBlocking

@WorkerThread
fun createNavigationRoutes(
    directionsResponseJson: String,
    routeRequestUrl: String,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
) = runBlocking {
    setupParsing(nativeRoute = false).parseDirectionsResponse(
        DirectionsResponseToParse.from(
            responseBody = directionsResponseJson.toDataRefJava(),
            routeRequest = routeRequestUrl,
            routerOrigin = routerOrigin,
        ),
    ).getOrThrow().routes
}

/**
 * This function is temporary used by an important customer.
 * Ping them if you need to modify that.
 * More details: https://mapbox.atlassian.net/browse/NAVAND-1765
 */
@ExperimentalPreviewMapboxNavigationAPI
@WorkerThread
fun createMatchedRoutes(
    mapMatchingResponse: String,
    requestUrl: String,
): Expected<Throwable, List<MapMatchingMatch>> {
    return NavigationRoute.createMatchedRoutes(
        mapMatchingResponse,
        requestUrl,
    )
}
