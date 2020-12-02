package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.Point

/**
 * @param nestedList nested arrays of legs -> steps -> points
 * @param flatList list of all points on the route.
 * The first and last point of adjacent steps overlap and are duplicated in this list.
 */
data class RoutePoints(val nestedList: List<List<List<Point>>>, val flatList: List<Point>)
