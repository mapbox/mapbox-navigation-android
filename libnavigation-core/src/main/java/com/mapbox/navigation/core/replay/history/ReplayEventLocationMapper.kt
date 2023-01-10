package com.mapbox.navigation.core.replay.history

import android.location.Location
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.util.Date

private const val MILLIS_PER_SECOND = 1000.0
private const val NANOS_PER_SECOND = 1e+9
private fun Double.secToMillis(): Long = (this * MILLIS_PER_SECOND).toLong()
private fun Double.secToNanos(): Long = (this * NANOS_PER_SECOND).toLong()

internal fun ReplayEventLocation.mapToLocation(
    eventTimeOffset: Double = time ?: 0.0,
    @VisibleForTesting currentTimeMilliseconds: Long = Date().time,
    @VisibleForTesting elapsedTimeNano: Long = SystemClock.elapsedRealtimeNanos()
): Location {
    val location = Location(provider)
    location.longitude = lon
    location.latitude = lat
    location.time = currentTimeMilliseconds + eventTimeOffset.secToMillis()
    location.elapsedRealtimeNanos = elapsedTimeNano + eventTimeOffset.secToNanos()
    accuracyHorizontal?.toFloat()?.let { location.accuracy = it }
    bearing?.toFloat()?.let { location.bearing = it }
    altitude?.let { location.altitude = it }
    speed?.toFloat()?.let { location.speed = it }
    return location
}
