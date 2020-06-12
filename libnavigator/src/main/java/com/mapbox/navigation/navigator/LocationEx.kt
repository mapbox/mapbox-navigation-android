package com.mapbox.navigation.navigator

import android.location.Location
import android.os.Build
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

    if (isCurrentSdkVersionEqualOrGreaterThan(Build.VERSION_CODES.O)) {
        it.bearingAccuracyDegrees = this.bearingAccuracy ?: 0f
        it.speedAccuracyMetersPerSecond = this.speedAccuracy ?: 0f
        it.verticalAccuracyMeters = this.verticalAccuracy ?: 0f
    }
}

internal fun Location.toFixLocation(date: Date): FixLocation {
    var bearingAccuracy: Float? = null
    var speedAccuracy: Float? = null
    var verticalAccuracy: Float? = null

    if (isCurrentSdkVersionEqualOrGreaterThan(Build.VERSION_CODES.O)) {
        bearingAccuracy = this.bearingAccuracyDegrees
        speedAccuracy = this.speedAccuracyMetersPerSecond
        verticalAccuracy = this.verticalAccuracyMeters
    }

    return FixLocation(
        Point.fromLngLat(this.longitude, this.latitude),
        date,
        this.speed,
        this.bearing,
        this.altitude.toFloat(),
        this.accuracy,
        this.provider,
        bearingAccuracy,
        speedAccuracy,
        verticalAccuracy
    )
}

private fun isCurrentSdkVersionEqualOrGreaterThan(sdkCode: Int): Boolean =
    Build.VERSION.SDK_INT >= sdkCode
