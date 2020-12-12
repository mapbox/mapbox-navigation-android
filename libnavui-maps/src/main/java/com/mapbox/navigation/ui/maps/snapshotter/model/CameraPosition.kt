package com.mapbox.navigation.ui.maps.snapshotter.model

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets

/**
 * A class that holds definitions for camera positions
 */
data class CameraPosition(
    val points: List<Point>,
    val insets: EdgeInsets,
    val bearing: Double,
    val pitch: Double
)
