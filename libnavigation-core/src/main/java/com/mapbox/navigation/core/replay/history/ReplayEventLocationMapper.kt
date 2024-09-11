package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.mapbox.common.location.Location
import java.util.Date

private const val MILLIS_PER_SECOND = 1000.0
private const val NANOS_PER_SECOND = 1e+9
private fun Double.secToMillis(): Long = (this * MILLIS_PER_SECOND).toLong()
private fun Double.secToNanos(): Long = (this * NANOS_PER_SECOND).toLong()

internal fun ReplayEventLocation.mapToLocation(
    eventTimeOffset: Double = time ?: 0.0,
    @VisibleForTesting currentTimeMilliseconds: Long = Date().time,
    @VisibleForTesting elapsedTimeNano: Long = SystemClock.elapsedRealtimeNanos(),
): Location {
    return Location.Builder()
        .source(provider)
        .longitude(lon)
        .latitude(lat)
        .timestamp(currentTimeMilliseconds + eventTimeOffset.secToMillis())
        .monotonicTimestamp(elapsedTimeNano + eventTimeOffset.secToNanos())
        .horizontalAccuracy(accuracyHorizontal)
        .bearing(bearing)
        .altitude(altitude)
        .speed(speed)
        .build()
}
