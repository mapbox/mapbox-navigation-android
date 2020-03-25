package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationFeedbackEvent(
    phoneState: PhoneState,
    metricsRouteProgress: MetricsRouteProgress
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they are should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val userId: String = phoneState.userId
    val feedbackId: String = phoneState.feedbackId
    val step: NavigationStepData = NavigationStepData(metricsRouteProgress)
    var feedbackType: String? = null
    var source: String? = null
    var description: String? = null
    var locationsBefore: Array<Location>? = emptyArray()
    var locationsAfter: Array<Location>? = emptyArray()
    var screenshot: String? = null

    override fun getEventName(): String = NavigationMetrics.FEEDBACK
}
