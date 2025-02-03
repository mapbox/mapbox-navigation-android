package com.mapbox.navigation.utils.internal

import com.mapbox.common.location.Location
import com.mapbox.geojson.Point

fun Location.toPoint(): Point {
    return Point.fromLngLat(this.longitude, this.latitude)
}
