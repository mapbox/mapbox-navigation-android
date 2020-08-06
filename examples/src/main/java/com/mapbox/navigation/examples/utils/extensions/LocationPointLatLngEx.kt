package com.mapbox.navigation.examples.utils.extensions

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

fun Point.toLocation(): Location {
    val location = Location("")
    location.latitude = this.latitude()
    location.longitude = this.longitude()
    return location
}

fun Location.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun LatLng.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun Point.toLatLng(): LatLng =
    if (this.hasAltitude())
        LatLng(this.latitude(), this.longitude(), this.altitude())
    else
        LatLng(this.latitude(), this.longitude())
