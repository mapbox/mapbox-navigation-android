package com.mapbox.navigation.navigator

import android.location.Location
import android.os.Build
import com.mapbox.geojson.Point
import com.mapbox.navigator.FixLocation
import java.util.Date

internal fun FixLocation.toLocation(): Location = Location(this.provider).also {
    it.latitude = coordinate.latitude()
    it.longitude = coordinate.longitude()
    it.time = time.time
    it.elapsedRealtimeNanos = monotonicTimestampNanoseconds

    speed?.run { it.speed = this }
    bearing?.run { it.bearing = this }
    altitude?.run { it.altitude = this.toDouble() }
    accuracyHorizontal?.run { it.accuracy = this }

    if (isCurrentSdkVersionEqualOrGreaterThan(Build.VERSION_CODES.O)) {
        bearingAccuracy?.run { it.bearingAccuracyDegrees = this }
        speedAccuracy?.run { it.speedAccuracyMetersPerSecond = this }
        verticalAccuracy?.run { it.verticalAccuracyMeters = this }
    }
}

internal fun Location.toFixLocation(): FixLocation {
    var bearingAccuracy: Float? = null
    var speedAccuracy: Float? = null
    var verticalAccuracy: Float? = null

    if (isCurrentSdkVersionEqualOrGreaterThan(Build.VERSION_CODES.O)) {
        bearingAccuracy = if (hasBearingAccuracy()) this.bearingAccuracyDegrees else null
        speedAccuracy = if (hasSpeedAccuracy()) this.speedAccuracyMetersPerSecond else null
        verticalAccuracy = if (hasVerticalAccuracy()) this.verticalAccuracyMeters else null
    }

    return FixLocation(
        Point.fromLngLat(longitude, latitude),
        elapsedRealtimeNanos,
        Date(time),
        if (hasSpeed()) speed else null,
        if (hasBearing()) bearing else null,
        if (hasAltitude()) altitude.toFloat() else null,
        if (hasAccuracy()) accuracy else null,
        provider,
        bearingAccuracy,
        speedAccuracy,
        verticalAccuracy
    )
}

private fun isCurrentSdkVersionEqualOrGreaterThan(sdkCode: Int): Boolean =
    Build.VERSION.SDK_INT >= sdkCode
