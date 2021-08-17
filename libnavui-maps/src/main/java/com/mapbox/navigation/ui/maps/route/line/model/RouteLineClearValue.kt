package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection

/**
 * Represents data used to remove the route line(s) from the map.
 *
 * @param primaryRouteSource a feature collection representing the primary route
 * @param altRoute1Source a feature collection representing an alternative route
 * @param altRoute2Source a feature collection representing an alternative route
 * @param waypointsSource a feature collection representing the origin and destination icons
 */
class RouteLineClearValue internal constructor(
    val primaryRouteSource: FeatureCollection,
    val altRoute1Source: FeatureCollection,
    val altRoute2Source: FeatureCollection,
    val waypointsSource: FeatureCollection,
)
