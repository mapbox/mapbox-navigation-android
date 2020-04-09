package com.mapbox.navigation.navigator

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import java.util.Date

internal fun FixLocation.toLocation(): Location = Location(this.provider).also {
    it.latitude = this.coordinate.latitude()
    it.longitude = this.coordinate.longitude()
    it.time = this.time.time
    it.speed = this.speed ?: 0f
    it.bearing = this.bearing ?: 0f
    it.altitude = this.altitude?.toDouble() ?: 0.0
    it.accuracy = this.accuracyHorizontal ?: 0f
}

internal fun Location.toFixLocation(date: Date) = FixLocation(
    Point.fromLngLat(this.longitude, this.latitude),
    date,
    this.speed,
    this.bearing,
    this.altitude.toFloat(),
    this.accuracy,
    this.provider
)
