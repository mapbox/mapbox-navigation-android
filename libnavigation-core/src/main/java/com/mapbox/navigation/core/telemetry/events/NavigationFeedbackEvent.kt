package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationFeedbackEvent(
    phoneState: PhoneState,
    navigationStepData: NavigationStepData,
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val userId: String = phoneState.userId
    val feedbackId: String = phoneState.feedbackId
    val step: NavigationStepData = navigationStepData
    var feedbackType: String? = null
    var source: String? = null
    var description: String? = null
    var locationsBefore: Array<TelemetryLocation>? = emptyArray()
    var locationsAfter: Array<TelemetryLocation>? = emptyArray()
    var screenshot: String? = null
    var feedbackSubType: Array<String>? = emptyArray()

    override fun getEventName(): String = NavigationMetrics.FEEDBACK

    override fun customFields(): Map<String, Value> = hashMapOf<String, Value>().also { fields ->
        fields["userId"] = userId.toValue()
        fields["feedbackId"] = feedbackId.toValue()
        fields["step"] = step.toValue()
        feedbackType?.let { fields["feedbackType"] = it.toValue() }
        source?.let { fields["source"] = it.toValue() }
        description?.let { fields["description"] = it.toValue() }
        locationsBefore?.let { fields["locationsBefore"] = it.toValue { toValue() } }
        locationsAfter?.let { fields["locationsAfter"] = it.toValue { toValue() } }
        screenshot?.let { fields["screenshot"] = it.toValue() }
        feedbackSubType?.let { fields["feedbackSubType"] = it.toValue { toValue() } }
    }
}
