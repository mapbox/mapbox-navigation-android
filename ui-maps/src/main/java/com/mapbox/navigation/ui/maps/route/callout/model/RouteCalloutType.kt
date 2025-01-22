package com.mapbox.navigation.ui.maps.route.callout.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Describes the possible callout types on the route line.
 */
@ExperimentalPreviewMapboxNavigationAPI
enum class RouteCalloutType {

    /**
     * Shows the route duration
     */
    RouteDurations,

    /**
     * Shows the relative diff between the main route and the alternative
     *
     */
    RelativeDurationsOnAlternative,
}
