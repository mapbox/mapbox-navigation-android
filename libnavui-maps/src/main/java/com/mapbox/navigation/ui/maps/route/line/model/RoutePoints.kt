package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.Point

/**
 * @param stepPoints nested arrays of legs -> steps -> points
 * @param flatList list of all points on the route.
 * The first and last point of adjacent steps overlap and are duplicated in this list. In some other cases
 * the step itself might have duplicate points (like arrival step) which is also reflected in this flat list.
 */
internal data class RoutePoints constructor(
    val stepPoints: List<List<List<Point>>>,
    val flatList: List<Point>,
)
