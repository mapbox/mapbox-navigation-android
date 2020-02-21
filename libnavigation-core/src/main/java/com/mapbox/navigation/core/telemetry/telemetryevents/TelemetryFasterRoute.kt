package com.mapbox.navigation.core.telemetry.telemetryevents
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
class TelemetryFasterRoute(
    val newDistanceRemaining: Int,
    val newDurationRemaining: Int = 0,
    val newGeometry: String? = null,
    val step: TelemetryStep? = null,
    var Metadata: TelemetryMetadata
) : MetricEvent, Event() {
    val event = "navigation.fasterRoute"
    override fun writeToParcel(dest: Parcel?, flags: Int) {
    }

    override fun describeContents() = 0
    override val metricName: String
        get() = NavigationMetrics.FASTER_ROUTE

    override fun toJson(gson: Gson) = gson.toJson(this)
}
