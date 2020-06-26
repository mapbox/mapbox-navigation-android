package com.mapbox.navigation.navigator

import android.location.Location
import android.os.Build
import android.os.SystemClock
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
        bearingAccuracy = if (hasBearingAccuracy()) this.bearingAccuracyDegrees else null
        speedAccuracy = if (hasSpeedAccuracy()) this.speedAccuracyMetersPerSecond else null
        verticalAccuracy = if (hasVerticalAccuracy()) this.verticalAccuracyMeters else null
    }

    var elapsed = SystemClock.elapsedRealtimeNanos();

    return FixLocation(
        Point.fromLngLat(longitude, latitude),
        date,
        if (hasSpeed()) speed else null,
        if (hasBearing()) bearing else null,
        if (hasAltitude()) altitude.toFloat() else null,
        if (hasAccuracy()) accuracy else null,
        "$time|$elapsedRealtimeNanos|$elapsed",
        bearingAccuracy,
        speedAccuracy,
        verticalAccuracy
    )
}

private fun isCurrentSdkVersionEqualOrGreaterThan(sdkCode: Int): Boolean =
    Build.VERSION.SDK_INT >= sdkCode
