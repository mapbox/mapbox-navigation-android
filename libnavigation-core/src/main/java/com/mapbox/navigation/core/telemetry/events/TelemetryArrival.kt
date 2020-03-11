package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional
@SuppressLint("ParcelCreator")
class TelemetryArrival(
    val arrivalTimestamp: String? = null,
    val rating: Int = -1,
    val comment: String? = null,
    val metadata: TelemetryMetadata
) : MetricEvent, Event() {
    val event = NavigationMetrics.ARRIVE

    override fun writeToParcel(dest: Parcel?, flags: Int) {}

    override fun describeContents() = 0

    override val metricName: String
        get() = NavigationMetrics.ARRIVE

    override fun toJson(gson: Gson) = gson.toJson(this)
}
