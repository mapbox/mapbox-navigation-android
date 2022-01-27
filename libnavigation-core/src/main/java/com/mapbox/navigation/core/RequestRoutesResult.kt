package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

@ExperimentalPreviewMapboxNavigationAPI
sealed class RequestRoutesResult {
    @ExperimentalPreviewMapboxNavigationAPI
    data class Successful(
        val routes: List<NavigationRoute>,
        val routerOrigin: RouterOrigin,
    ) : RequestRoutesResult()

    @ExperimentalPreviewMapboxNavigationAPI
    data class Failed(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RequestRoutesResult()
}
