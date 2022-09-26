package com.mapbox.navigation.dropin

import com.mapbox.api.directions.v5.models.RouteOptions

/**
 * Interface allowing to modify [RouteOptions.Builder] during a route request with [NavigationView].
 */
fun interface RouteOptionsInterceptor {

    /**
     * Allows to modify [RouteOptions.Builder] before a route request is sent.
     *
     * @param builder with initial route options
     */
    fun intercept(builder: RouteOptions.Builder): RouteOptions.Builder
}
