package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.navigation.base.internal.metric.MetricEventInternal
import com.mapbox.navigation.base.metrics.NavigationMetrics

/**
 * Navigation free drive event.
 *
 * Note: class doesn't have driverMode property because it's always FreeDrive.
 *
 * @property sessionIdentifier unique id of FreeDrive(same for start and stop events)
 * @property startTimestamp start time FreeDrive event
 * @property navigatorSessionIdentifier group id of modes(FreeDrive/ActiveGuidance) under one Telemetry session
 */
@SuppressLint("ParcelCreator")
internal class NavigationFreeDriveEvent(
    phoneState: PhoneState,
) : MetricEventInternal {

    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val version = "2.2"
    val created: String = TelemetrySystemUtils.obtainCurrentDate() // Schema pattern
    val volumeLevel: Int = phoneState.volumeLevel
    val batteryLevel: Int = phoneState.batteryLevel
    val screenBrightness: Int = phoneState.screenBrightness
    val batteryPluggedIn: Boolean = phoneState.isBatteryPluggedIn
    val connectivity: String = phoneState.connectivity
    val audioType: String = phoneState.audioType
    val applicationState: String = phoneState.applicationState // Schema minLength 1
    val event: String = NavigationMetrics.FREE_DRIVE
    var eventVersion: Int = 0
    var locationEngine: String? = null
    var percentTimeInPortrait: Int = 0
    var percentTimeInForeground: Int = 0
    var simulation: Boolean = false

    var navigatorSessionIdentifier: String? = null // group id of modes under one Telemetry session
    var startTimestamp: String? = null // mode start time
    var sessionIdentifier: String? = null // mode id

    var location: TelemetryLocation? = null
    var eventType: String? = null
    var appMetadata: AppMetadata? = null

    override val metricName: String
        get() = NavigationMetrics.FREE_DRIVE

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override fun toValue(): Value {
        val fields = hashMapOf<String, Value>()

        fields["version"] = version.toValue()
        fields["created"] = created.toValue()
        fields["volumeLevel"] = volumeLevel.toValue()
        fields["batteryLevel"] = batteryLevel.toValue()
        fields["screenBrightness"] = screenBrightness.toValue()
        fields["batteryPluggedIn"] = batteryPluggedIn.toValue()
        fields["connectivity"] = connectivity.toValue()
        fields["audioType"] = audioType.toValue()
        fields["applicationState"] = applicationState.toValue()
        fields["event"] = event.toValue()
        fields["eventVersion"] = eventVersion.toValue()
        locationEngine?.let { fields["locationEngine"] = it.toValue() }
        fields["percentTimeInPortrait"] = percentTimeInPortrait.toValue()
        fields["percentTimeInForeground"] = percentTimeInForeground.toValue()
        fields["simulation"] = simulation.toValue()
        navigatorSessionIdentifier?.let { fields["navigatorSessionIdentifier"] = it.toValue() }
        startTimestamp?.let { fields["startTimestamp"] = it.toValue() }
        sessionIdentifier?.let { fields["sessionIdentifier"] = it.toValue() }
        location?.let { fields["location"] = it.toValue() }
        eventType?.let { fields["eventType"] = it.toValue() }
        appMetadata?.let { fields["appMetadata"] = it.toValue() }

        return Value.valueOf(fields)
    }
}

internal enum class FreeDriveEventType(val type: String) {
    START("start"),
    STOP("stop"),
}
