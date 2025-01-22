package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection

/**
 * Provides information needed to draw a route.
 *
 * @param featureCollection the routes geometry
 * @param dynamicData dynamic data to style the route line
 */
internal data class RouteLineData(
    val featureCollection: FeatureCollection,
    val dynamicData: RouteLineDynamicData? = null,
)
