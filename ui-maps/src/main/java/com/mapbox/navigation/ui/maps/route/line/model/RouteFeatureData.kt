package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * An association between a DirectionsRoute, FeatureCollection
 * and LineString.
 *
 * @param route a DirectionsRoute
 * @param reversedFeatureCollection a FeatureCollection created using the route, reversed (ready to be rendered)
 * @param coordinatesCount number of coordinates in geometry
 */
internal data class RouteFeatureData(
    val route: NavigationRoute,
    val reversedFeatureCollection: FeatureCollection,
    val coordinatesCount: Int,
)
