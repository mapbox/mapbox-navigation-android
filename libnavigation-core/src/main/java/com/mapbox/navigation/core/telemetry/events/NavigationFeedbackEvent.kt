package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
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
    var locationsBefore: Array<FeedbackLocation>? = emptyArray()
    var locationsAfter: Array<FeedbackLocation>? = emptyArray()
    var screenshot: String? = null
    var feedbackSubType: Array<String>? = emptyArray()
    var appMetadata: AppMetadata? = null

    override fun getEventName(): String = NavigationMetrics.FEEDBACK
}

internal data class FeedbackLocation(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lng") val longitude: Double,
    @SerializedName("speed") val speed: Float,
    @SerializedName("course") val bearing: Float,
    @SerializedName("altitude") val altitude: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("horizontalAccuracy") val horizontalAccuracy: Float,
    @SerializedName("verticalAccuracy") val verticalAccuracy: Float
)
