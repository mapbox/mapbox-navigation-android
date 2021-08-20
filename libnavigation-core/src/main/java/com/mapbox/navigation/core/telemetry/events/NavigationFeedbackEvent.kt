package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent

@SuppressLint("ParcelCreator")
internal class NavigationFeedbackEvent(
    phoneState: PhoneState,
    metricsRouteProgress: MetricsRouteProgress
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val userId: String = phoneState.userId
    val feedbackId: String = phoneState.feedbackId
    val step: NavigationStepData = NavigationStepData(metricsRouteProgress)
    var feedbackType: String? = null
    var source: String? = null
    var description: String? = null
    var locationsBefore: Array<TelemetryLocation>? = emptyArray()
    var locationsAfter: Array<TelemetryLocation>? = emptyArray()
    var screenshot: String? = null
    var feedbackSubType: Array<String>? = emptyArray()

    override fun getEventName(): String = NavigationMetrics.FEEDBACK

    fun getCachedNavigationFeedbackEvent() =
        CachedNavigationFeedbackEvent(
            feedbackId,
            feedbackType ?: "",
            screenshot ?: "",
            description,
            HashSet(feedbackSubType?.toSet() ?: emptySet())
        )

    fun update(cachedNavigationFeedbackEvent: CachedNavigationFeedbackEvent) {
        feedbackType = cachedNavigationFeedbackEvent.feedbackType
        description = cachedNavigationFeedbackEvent.description
        feedbackSubType = cachedNavigationFeedbackEvent.feedbackSubType.toTypedArray()
    }
}
