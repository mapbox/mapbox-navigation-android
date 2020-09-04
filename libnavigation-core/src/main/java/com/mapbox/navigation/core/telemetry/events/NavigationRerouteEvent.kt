package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationRerouteEvent(
    phoneState: PhoneState,
    metricsRouteProgress: MetricsRouteProgress
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they are should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    var newDistanceRemaining: Int = 0
    var newDurationRemaining: Int = 0
    val feedbackId: String = phoneState.feedbackId
    var newGeometry: String? = null
    val step: NavigationStepData = NavigationStepData(metricsRouteProgress)
    var secondsSinceLastReroute: Int = 0
    var locationsBefore: Array<TelemetryLocation>? = emptyArray()
    var locationsAfter: Array<TelemetryLocation>? = emptyArray()
    var screenshot: String? = null

    override fun getEventName(): String = NavigationMetrics.REROUTE
}
