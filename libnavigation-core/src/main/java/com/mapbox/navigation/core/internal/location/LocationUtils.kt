package com.mapbox.navigation.core.internal.location

import android.os.Build
import com.mapbox.bindgen.Value
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationExtraKeys
import com.mapbox.common.toValue

@Suppress("UNCHECKED_CAST")
internal fun android.location.Location.toCommonLocation(): Location {
    val builder = Location.Builder().latitude(latitude).longitude(longitude)
        .timestamp(time).monotonicTimestamp(this.elapsedRealtimeNanos)

    if (this.hasAccuracy()) builder.horizontalAccuracy(this.accuracy.toDouble())
    if (this.hasAltitude()) builder.altitude(this.altitude)
    if (this.hasBearing()) builder.bearing(this.bearing.toDouble())
    if (this.hasSpeed()) builder.speed(this.speed.toDouble())
    this.provider?.let {
        builder.source(it)
    }

    if (Build.VERSION.SDK_INT >= 26) {
        if (hasVerticalAccuracy()) builder.verticalAccuracy(
            verticalAccuracyMeters.toDouble()
        )
        if (hasSpeedAccuracy()) builder.speedAccuracy(
            speedAccuracyMetersPerSecond.toDouble()
        )
        if (hasBearingAccuracy()) builder.bearingAccuracy(
            bearingAccuracyDegrees.toDouble()
        )
    }

    var extra: Value? = extras?.toValue()

    if (isMock(this)) {
        if (extra == null) {
            extra = Value.valueOf(hashMapOf())
        }
        val content = extra.contents as HashMap<String, Value>
        content[LocationExtraKeys.IS_MOCK] = Value.valueOf(true)
    }

    extra?.let {
        builder.extra(it)
    }

    return builder.build()
}

private fun isMock(location: android.location.Location): Boolean {
    // TODO: add isMock() when targeting api level 31
    // See https://developer.android.com/reference/android/location/Location#isMock()
    return location.isFromMockProvider
}

internal fun Location.toAndroidLocation(): android.location.Location {
    val androidLocation = android.location.Location(this.source)
    androidLocation.latitude = this.latitude
    androidLocation.longitude = this.longitude
    androidLocation.time = this.timestamp
    this.monotonicTimestamp?.let { androidLocation.elapsedRealtimeNanos = it }
    this.altitude?.let { androidLocation.altitude = it }
    this.horizontalAccuracy?.let { androidLocation.accuracy = it.toFloat() }
    this.speed?.let { androidLocation.speed = it.toFloat() }
    this.bearing?.let { androidLocation.bearing = it.toFloat() }

    if (Build.VERSION.SDK_INT >= 26) {
        this.verticalAccuracy?.let {
            androidLocation.verticalAccuracyMeters = it.toFloat()
        }
        this.speedAccuracy?.let {
            androidLocation.speedAccuracyMetersPerSecond = it.toFloat()
        }
        this.bearingAccuracy?.let {
            androidLocation.bearingAccuracyDegrees = it.toFloat()
        }
    }

    (extra?.contents as? HashMap<*, *>)?.let { map ->
        map[LocationExtraKeys.IS_MOCK]?.let { isMock ->
            ((isMock as? Value)?.contents as? Boolean)?.let {
                if (it) {
                    runCatching {
                        android.location.Location::class.java.getDeclaredMethod(
                            "setIsFromMockProvider",
                            Boolean::class.java
                        ).invoke(androidLocation, isMock.contents)
                    }
                }
            }
        }
    }

    return androidLocation
}
