package com.mapbox.navigation.metrics

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.Metric
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

object MetricEventMapper {

    fun mapMetricEventToTelemetryEvent(@Metric metric: String, metricEvent: MetricEvent): Event? =
        when (metric) {
            DirectionsMetrics.ROUTE_RETRIEVAL,
            NavigationMetrics.ARRIVE,
            NavigationMetrics.CANCEL_SESSION,
            NavigationMetrics.DEPART,
            NavigationMetrics.REROUTE,
            NavigationMetrics.FEEDBACK,
            NavigationMetrics.INITIAL_GPS,
            NavigationMetrics.APP_USER_TURNSTILE -> metricEvent as Event
            else -> null
        }
}
