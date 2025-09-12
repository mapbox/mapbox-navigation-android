package com.mapbox.navigation.base.internal.time

import android.os.SystemClock
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MonotonicTimestampProvider {

    /**
     * NN requires monotonicTimestampNanoseconds in sensor data to be based
     * on the same source as [com.mapbox.navigator.FixLocation.monotonicTimestampNanoseconds],
     * which is in turn based on [Location#getElapsedRealtimeNanos()](https://developer.android.com/reference/android/location/Location#getElapsedRealtimeNanos()),
     *
     * @see [com.mapbox.navigation.core.navigator.toFixLocation]
     */
    fun getLocationBasedMonotonicTimestamp() = SystemClock.elapsedRealtimeNanos()
}
