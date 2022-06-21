package com.mapbox.navigation.core.telemetry.events.internal

import android.annotation.SuppressLint
import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.core.telemetry.events.PhoneState

@SuppressLint("ParcelCreator")
internal class NavigationCustomEvents: Event(), MetricEvent {

    val version = "2.4"
    val event: String = NavigationMetrics.CUSTOM_EVENT

    var type: String = CustomEventType.CORE.type
    var customEventVersion = "1.0"
    var payload: String? = null

    override val metricName: String
        get() = NavigationMetrics.CUSTOM_EVENT

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }
}

enum class CustomEventType(val type: String) {
    UI("ui"),
    CORE("core"),
    DropInUI("dropInUI"),
    AndroidAuto("androidAuto")
}
