package com.mapbox.navigation.core.telemetry.telemetryevents

import android.annotation.SuppressLint
import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.metrics.internal.utils.extentions.MetricEvent
import com.mapbox.navigation.metrics.internal.utils.extentions.NavigationMetrics

/**
 * Documentation is here [https://paper.dropbox.com/doc/Navigation-Telemetry-Events-V1--AuUz~~~rEVK7iNB3dQ4_tF97Ag-iid3ZImnt4dsW7Z6zC3Lc]
 */

// Defaulted values are optional

@SuppressLint("ParcelCreator")
class TelemetryDepartureEvent(
    val Metadata: TelemetryMetadata
) : MetricEvent, Event() { // Data common to all telemetry events
    val event = "navigation.depart"
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
    override fun describeContents(): Int = 0
    override val metricName: String
        get() = NavigationMetrics.DEPART

    override fun toJson(gson: Gson) = gson.toJson(this)
}
