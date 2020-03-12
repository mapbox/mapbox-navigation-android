package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.Keep
import com.mapbox.navigation.base.metrics.NavigationMetrics

@Keep
@SuppressLint("ParcelCreator")
internal class NavigationRerouteEvent(
    phoneState: PhoneState,
    rerouteEvent: RerouteEvent,
    metricsRouteProgress: MetricsRouteProgress
) : NavigationEvent(phoneState) {
    /*
     * Don't remove any fields, cause they are should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val newDistanceRemaining: Int = rerouteEvent.newDistanceRemaining
    val newDurationRemaining: Int = rerouteEvent.newDurationRemaining
    val feedbackId: String = phoneState.feedbackId
    val newGeometry: String = rerouteEvent.newRouteGeometry
    val step: NavigationStepData = NavigationStepData(metricsRouteProgress)
    var secondsSinceLastReroute: Int = 0
    var locationsBefore: Array<Location>? = emptyArray()
    var locationsAfter: Array<Location>? = emptyArray()
    var screenshot: String? = null

    override fun getEventName(): String = NavigationMetrics.REROUTE
}
