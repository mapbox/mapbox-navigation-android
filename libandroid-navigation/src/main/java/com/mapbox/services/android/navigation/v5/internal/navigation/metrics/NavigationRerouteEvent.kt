package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.annotation.SuppressLint
import android.location.Location
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetrics

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
    var locationsBefore: Array<Location>? = null
    var locationsAfter: Array<Location>? = null
    var screenshot: String? = null

    override fun getEventName(): String = NavigationMetrics.REROUTE
}
