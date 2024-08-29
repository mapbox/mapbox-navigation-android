package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationRerouteEvent(
    phoneState: PhoneState,
    navigationStepData: NavigationStepData,
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    var newDistanceRemaining: Int = 0
    var newDurationRemaining: Int = 0
    val feedbackId: String = phoneState.feedbackId
    var newGeometry: String? = null
    val step: NavigationStepData = navigationStepData
    var secondsSinceLastReroute: Int = 0
    var locationsBefore: Array<TelemetryLocation>? = emptyArray()
    var locationsAfter: Array<TelemetryLocation>? = emptyArray()
    var screenshot: String? = null

    override fun getEventName(): String = NavigationMetrics.REROUTE

    override fun customFields(): Map<String, Value>? = hashMapOf<String, Value>().also { fields ->
        fields["newDistanceRemaining"] = newDistanceRemaining.toValue()
        fields["newDurationRemaining"] = newDurationRemaining.toValue()
        fields["feedbackId"] = feedbackId.toValue()
        newGeometry?.let { fields["newGeometry"] = it.toValue() }
        fields["step"] = step.toValue()
        fields["secondsSinceLastReroute"] = secondsSinceLastReroute.toValue()
        locationsBefore?.let { fields["locationsBefore"] = it.toValue { toValue() } }
        locationsAfter?.let { fields["locationsAfter"] = it.toValue { toValue() } }
        screenshot?.let { fields["screenshot"] = it.toValue() }
    }
}
