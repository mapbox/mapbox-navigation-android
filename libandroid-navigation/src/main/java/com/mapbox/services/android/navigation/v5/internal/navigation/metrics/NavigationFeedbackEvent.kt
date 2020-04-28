package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetrics

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
    var locationsBefore: Array<Location>? = null
    var locationsAfter: Array<Location>? = null
    var screenshot: String? = null
    var feedbackSubType: Array<String>? = emptyArray()

    override fun getEventName(): String = NavigationMetrics.FEEDBACK
}
