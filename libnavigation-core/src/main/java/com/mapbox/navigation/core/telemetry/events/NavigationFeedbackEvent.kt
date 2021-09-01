package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationFeedbackEvent(
    phoneState: PhoneState,
    navigationStepData: NavigationStepData
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
}
