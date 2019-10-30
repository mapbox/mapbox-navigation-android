package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.Gson
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationPerformanceEvent
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent

@SuppressLint("ParcelCreator")
internal data class InitialGpsEvent(
    private val elapsedTime: Double,
    @Transient private val sessionId: String,
    @Transient override var metadata: NavigationPerformanceMetadata
) : NavigationPerformanceEvent(sessionId, NavigationMetrics.INITIAL_GPS, metadata), MetricEvent, Parcelable {

    companion object {
        private const val TIME_TO_FIRST_GPS = "time_to_first_gps"
    }

    init {
        addCounter(DoubleCounter(TIME_TO_FIRST_GPS, elapsedTime))
    }

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override val metric: String
        get() = NavigationMetrics.INITIAL_GPS
}
