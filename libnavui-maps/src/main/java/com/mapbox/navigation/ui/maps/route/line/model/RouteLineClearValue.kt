package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection

/**
 * Represents data used to remove the route line(s) from the map.
 *
 * @param primaryRouteSource a feature collection representing the primary route
 * @param alternativeRouteSourceSources feature collections representing alternative routes
 * @param waypointsSource a feature collection representing the origin and destination icons
 */
class RouteLineClearValue internal constructor(
    val primaryRouteSource: FeatureCollection,
    val alternativeRouteSourceSources: List<FeatureCollection>,
    val waypointsSource: FeatureCollection,
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteLineClearValue(
        primaryRouteSource,
        alternativeRouteSourceSources,
        waypointsSource
    )

    /**
     * Represents mutable data used to remove the route line(s) from the map.
     *
     * @param primaryRouteSource a feature collection representing the primary route
     * @param alternativeRouteSourceSources feature collections representing alternative routes
     * @param waypointsSource a feature collection representing the origin and destination icons
     */
    class MutableRouteLineClearValue internal constructor(
        var primaryRouteSource: FeatureCollection,
        var alternativeRouteSourceSources: List<FeatureCollection>,
        var waypointsSource: FeatureCollection,
    ) {

        /**
         * @return a RouteLineClearValue
         */
        fun toImmutableValue() = RouteLineClearValue(
            primaryRouteSource,
            alternativeRouteSourceSources,
            waypointsSource
        )
    }
}
