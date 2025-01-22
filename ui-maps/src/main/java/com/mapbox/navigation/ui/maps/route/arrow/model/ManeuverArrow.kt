package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.Point

/**
 * Represents a maneuver arrow that can be added to the map during navigation. An arrow is made up
 * of two or more points. The arrowhead will be placed at the last point in the collection. The
 * direction of the arrowhead will be calculated based on the bearing from the second to last point
 * to the last point.
 *
 * @param points a collection of points defining the arrow
 */
class ManeuverArrow(val points: List<Point>)
