package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.os.Parcelable
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationPerformanceEvent

@SuppressLint("ParcelCreator")
internal data class InitialGpsEvent(
    private val elapsedTime: Double,
    @Transient private val sessionId: String,
    @Transient override var metadata: NavigationPerformanceMetadata
) : NavigationPerformanceEvent(sessionId, INITIAL_GPS_EVENT_NAME, metadata), Parcelable {

    companion object {
        private const val TIME_TO_FIRST_GPS = "time_to_first_gps"
        private const val INITIAL_GPS_EVENT_NAME = "initial_gps_event"
    }

    init {
        addCounter(DoubleCounter(TIME_TO_FIRST_GPS, elapsedTime))
    }
}
