package com.mapbox.navigation.core.telemetry.events

import android.os.Build
import androidx.annotation.CallSuper
import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.navigation.base.internal.metric.MetricEventInternal
import com.mapbox.navigation.core.BuildConfig

/**
 * Base Event class for navigation events, contains common properties.
 *
 * @property driverMode one of [FeedbackEvent.DriverMode]
 * @property sessionIdentifier id of [driverMode]
 * @property startTimestamp start time of [driverMode]
 * @property navigatorSessionIdentifier group id of modes under one Telemetry session
 */
internal abstract class NavigationEvent(
    phoneState: PhoneState,
) : MetricEventInternal {

    private companion object {
        private val OPERATING_SYSTEM = "Android - ${Build.VERSION.RELEASE}"
    }

    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val version = "2.2"
    val operatingSystem: String = OPERATING_SYSTEM
    val device: String? = Build.MODEL
    val sdkVersion: String = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
    val created: String = TelemetrySystemUtils.obtainCurrentDate() // Schema pattern
    val volumeLevel: Int = phoneState.volumeLevel
    val batteryLevel: Int = phoneState.batteryLevel
    val screenBrightness: Int = phoneState.screenBrightness
    val batteryPluggedIn: Boolean = phoneState.isBatteryPluggedIn
    val connectivity: String = phoneState.connectivity
    val audioType: String = phoneState.audioType
    val applicationState: String = phoneState.applicationState // Schema minLength 1
    val event: String = getEventName()

    var sdkIdentifier: String? = null

    var navigatorSessionIdentifier: String? = null
    var startTimestamp: String? = null
    var driverMode: String? = null
    var sessionIdentifier: String? = null
    var geometry: String? = null
    var profile: String? = null
    var originalRequestIdentifier: String? = null
    var requestIdentifier: String? = null
    var originalGeometry: String? = null
    var locationEngine: String? = null
    var tripIdentifier: String? = null
    var lat: Double = 0.toDouble()
    var lng: Double = 0.toDouble()
    var simulation: Boolean = false
    var absoluteDistanceToDestination: Int = 0
    var percentTimeInPortrait: Int = 0
    var percentTimeInForeground: Int = 0
    var distanceCompleted: Int = 0
    var distanceRemaining: Int = 0
    var durationRemaining: Int = 0
    var eventVersion: Int = 0
    var estimatedDistance: Int = 0
    var estimatedDuration: Int = 0
    var rerouteCount: Int = 0
    var originalEstimatedDistance: Int = 0
    var originalEstimatedDuration: Int = 0
    var stepCount: Int = 0
    var originalStepCount: Int = 0
    var legIndex: Int = 0
    var legCount: Int = 0
    var stepIndex: Int = 0
    var voiceIndex: Int = 0
    var bannerIndex: Int = 0
    var totalStepCount: Int = 0
    var appMetadata: AppMetadata? = null

    internal abstract fun getEventName(): String

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override val metricName: String
        get() = getEventName()

    @CallSuper
    override fun toValue(): Value {
        val fields = hashMapOf<String, Value>()

        fields["version"] = version.toValue()
        fields["operatingSystem"] = operatingSystem.toValue()
        device?.let { fields["device"] = it.toValue() }
        fields["sdkVersion"] = sdkVersion.toValue()
        fields["created"] = created.toValue()
        fields["volumeLevel"] = volumeLevel.toValue()
        fields["batteryLevel"] = batteryLevel.toValue()
        fields["screenBrightness"] = screenBrightness.toValue()
        fields["batteryPluggedIn"] = batteryPluggedIn.toValue()
        fields["connectivity"] = connectivity.toValue()
        fields["audioType"] = audioType.toValue()
        fields["applicationState"] = applicationState.toValue()
        fields["event"] = event.toValue()

        sdkIdentifier?.let { fields["sdkIdentifier"] = it.toValue() }

        navigatorSessionIdentifier?.let { fields["navigatorSessionIdentifier"] = it.toValue() }
        startTimestamp?.let { fields["startTimestamp"] = it.toValue() }
        driverMode?.let { fields["driverMode"] = it.toValue() }
        sessionIdentifier?.let { fields["sessionIdentifier"] = it.toValue() }
        geometry?.let { fields["geometry"] = it.toValue() }
        profile?.let { fields["profile"] = it.toValue() }
        originalRequestIdentifier?.let { fields["originalRequestIdentifier"] = it.toValue() }
        requestIdentifier?.let { fields["requestIdentifier"] = it.toValue() }
        originalGeometry?.let { fields["originalGeometry"] = it.toValue() }
        locationEngine?.let { fields["locationEngine"] = it.toValue() }
        tripIdentifier?.let { fields["tripIdentifier"] = it.toValue() }
        fields["lat"] = lat.toValue()
        fields["lng"] = lng.toValue()
        fields["simulation"] = simulation.toValue()
        fields["absoluteDistanceToDestination"] = absoluteDistanceToDestination.toValue()
        fields["percentTimeInPortrait"] = percentTimeInPortrait.toValue()
        fields["percentTimeInForeground"] = percentTimeInForeground.toValue()
        fields["distanceCompleted"] = distanceCompleted.toValue()
        fields["distanceRemaining"] = distanceRemaining.toValue()
        fields["durationRemaining"] = durationRemaining.toValue()
        fields["eventVersion"] = eventVersion.toValue()
        fields["estimatedDistance"] = estimatedDistance.toValue()
        fields["estimatedDuration"] = estimatedDuration.toValue()
        fields["rerouteCount"] = rerouteCount.toValue()
        fields["originalEstimatedDistance"] = originalEstimatedDistance.toValue()
        fields["originalEstimatedDuration"] = originalEstimatedDuration.toValue()
        fields["stepCount"] = stepCount.toValue()
        fields["originalStepCount"] = originalStepCount.toValue()
        fields["legIndex"] = legIndex.toValue()
        fields["legCount"] = legCount.toValue()
        fields["stepIndex"] = stepIndex.toValue()
        fields["voiceIndex"] = voiceIndex.toValue()
        fields["bannerIndex"] = bannerIndex.toValue()
        fields["totalStepCount"] = totalStepCount.toValue()
        appMetadata?.let { fields["appMetadata"] = it.toValue() }

        fields.putAll(customFields() ?: emptyMap())

        return Value.valueOf(fields)
    }

    /**
     * Provide custom fields here or `null` if they are not needed
     */
    protected abstract fun customFields(): Map<String, Value>?
}
