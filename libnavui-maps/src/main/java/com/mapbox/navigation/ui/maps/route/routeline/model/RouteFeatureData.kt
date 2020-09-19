package com.mapbox.navigation.ui.maps.route.routeline.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString

/**
 * An association between a DirectionsRoute, FeatureCollection
 * and LineString.
 *
 * @param route a DirectionsRoute
 * @param featureCollection a FeatureCollection created using the route
 * @param lineString a LineString derived from the route's geometry.
 */
data class RouteFeatureData(
    val route: DirectionsRoute,
    val featureCollection: FeatureCollection,
    val lineString: LineString
)
