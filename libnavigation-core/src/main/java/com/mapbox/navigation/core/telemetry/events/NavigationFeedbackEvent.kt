package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
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
    val version = "2.1"
    val userId: String = phoneState.userId
    val feedbackId: String = phoneState.feedbackId
    val step: NavigationStepData = NavigationStepData(metricsRouteProgress)
    var feedbackType: String? = null
    var source: String? = null
    var description: String? = null
    var locationsBefore: Array<TelemetryLocation>? = emptyArray()
    var locationsAfter: Array<TelemetryLocation>? = emptyArray()
    var screenshot: String? = null
    var feedbackSubType: Array<String> = emptyArray()
    var appMetadata: AppMetadata? = null

    override fun getEventName(): String = NavigationMetrics.FEEDBACK

    fun getCachedNavigationFeedbackEvent() =
        CachedNavigationFeedbackEvent(
            feedbackId,
            feedbackType ?: "",
            description,
            screenshot ?: "",
            HashSet(feedbackSubType.toSet())
        )

    fun update(cachedNavigationFeedbackEvent: CachedNavigationFeedbackEvent) {
        feedbackType = cachedNavigationFeedbackEvent.feedbackType
        description = cachedNavigationFeedbackEvent.description
        feedbackSubType = cachedNavigationFeedbackEvent.feedbackSubType.toTypedArray()
    }
}
