package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import android.os.Build
import android.os.Parcel
import androidx.annotation.CallSuper
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationCustomEvent : Event(), MetricEvent {

    private companion object {
        private val OPERATING_SYSTEM = "Android - ${Build.VERSION.RELEASE}"
    }

    var type: String = ""
    var payload: String? = null
    val version: String = "2.4"
    var customEventVersion: String = "1.0.0"
    val event: String = NavigationMetrics.CUSTOM_EVENT

    val created: String = TelemetryUtils.obtainCurrentDate()
    var createdMonotime: Int = 0
    val operatingSystem: String = OPERATING_SYSTEM
    val device: String? = Build.MODEL
    var driverMode: String = ""

    // Setting driverModeId to the following to match the pattern: "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}?"
    val driverModeId: String = "00000000-0000-0000-0000-000000000000"
    val driverModeStartTimestamp: String = "non_valid"
    var driverModeStartTimestampMonotime: Int = 0
    var sdkIdentifier: String? = null
    var eventVersion: Int = 0
    var simulation: Boolean = false
    var locationEngine: String? = null
    var lat: Double = 0.toDouble()
    var lng: Double = 0.toDouble()

    override val metricName: String
        get() = NavigationMetrics.CUSTOM_EVENT

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }

    @CallSuper
    override fun toValue(): Value {
        val fields = hashMapOf<String, Value>()

        payload?.let { fields["payload"] = it.toValue() }
        fields["version"] = version.toValue()
        fields["customEventVersion"] = customEventVersion.toValue()
        fields["event"] = event.toValue()
        fields["created"] = created.toValue()
        fields["createdMonotime"] = createdMonotime.toValue()
        fields["operatingSystem"] = operatingSystem.toValue()
        device?.let { fields["device"] = it.toValue() }
        fields["driverMode"] = driverMode.toValue()
        fields["driverModeId"] = driverModeId.toValue()
        fields["driverModeStartTimestamp"] = driverModeStartTimestamp.toValue()
        fields["driverModeStartTimestampMonotime"] = driverModeStartTimestampMonotime.toValue()
        sdkIdentifier?.let { fields["sdkIdentifier"] = it.toValue() }
        fields["eventVersion"] = eventVersion.toValue()
        fields["simulation"] = simulation.toValue()
        locationEngine?.let { fields["locationEngine"] = it.toValue() }
        fields["lat"] = lat.toValue()
        fields["lng"] = lng.toValue()
        return Value.valueOf(fields)
    }
}
