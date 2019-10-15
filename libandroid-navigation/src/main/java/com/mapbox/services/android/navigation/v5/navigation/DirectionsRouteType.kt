package com.mapbox.services.android.navigation.v5.navigation

/**
 * Used with [MapboxNavigation.startNavigation].
 */
enum class DirectionsRouteType {

    /**
     * This value means [MapboxNavigation] will consider the entire route with
     * [MapboxNavigation.startNavigation].  This value
     * should be used in off-route scenarios or any other scenario when you need a completely new route object.
     *
     * Please note, this is the default value for [MapboxNavigation.startNavigation].
     */
    NEW_ROUTE,

    /**
     * This value means [MapboxNavigation] will only consider annotation data with
     * [MapboxNavigation.startNavigation].  This value can be used with
     * [RouteRefresh] to provide up-to-date ETAs and congestion data while navigating.
     */
    FRESH_ROUTE
}
