package com.mapbox.navigation.ui.maps.camera.utils

import android.location.Location
import com.mapbox.geojson.Point

internal fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

internal fun Location.toPoint(): Point {
    return Point.fromLngLat(this.longitude, this.latitude)
}
