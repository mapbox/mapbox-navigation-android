package com.mapbox.navigation.ui.maps.snapshotter.model

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets

/**
 * A class that defines camera positions.
 * @property points list of [Point]
 * @property insets object defining padding
 * @property bearing camera bearing
 * @property pitch camera pitch
 */
internal data class CameraPosition(
    val points: List<Point>,
    val insets: EdgeInsets,
    val bearing: Double,
    val pitch: Double
)
