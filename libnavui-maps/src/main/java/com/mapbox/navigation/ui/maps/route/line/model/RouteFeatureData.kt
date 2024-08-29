package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * An association between a DirectionsRoute, FeatureCollection
 * and LineString.
 *
 * @param route a DirectionsRoute
 * @param featureCollection a FeatureCollection created using the route
 * @param lineString a LineString derived from the route's geometry.
 */
internal data class RouteFeatureData constructor(
    val route: NavigationRoute,
    val featureCollection: FeatureCollection,
    val lineString: LineString,
)
