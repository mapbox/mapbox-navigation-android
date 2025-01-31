package com.mapbox.navigation.base.internal.route

import androidx.annotation.WorkerThread
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.MapMatchingMatch
import com.mapbox.navigation.base.route.NavigationRoute

@WorkerThread
fun createNavigationRoutes(
    directionsResponseJson: String,
    routeRequestUrl: String,
    @com.mapbox.navigation.base.route.RouterOrigin
    routerOrigin: String,
) = NavigationRoute.create(
    directionsResponseJson,
    routeRequestUrl,
    routerOrigin,
)

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
