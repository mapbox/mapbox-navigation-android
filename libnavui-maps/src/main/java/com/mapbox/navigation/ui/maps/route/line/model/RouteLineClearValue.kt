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
)
