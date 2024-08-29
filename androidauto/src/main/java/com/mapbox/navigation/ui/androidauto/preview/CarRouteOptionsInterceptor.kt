package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * This allows you to change the route options used by Android Auto for requesting a route.
 */
fun interface CarRouteOptionsInterceptor {

    /**
     * Called before the route is requested.
     *
     * @param builder with initial route options
     */
    fun intercept(builder: RouteOptions.Builder): RouteOptions.Builder
}
