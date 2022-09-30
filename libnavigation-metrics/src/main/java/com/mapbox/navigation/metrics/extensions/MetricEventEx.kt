package com.mapbox.navigation.metrics.extensions

import com.mapbox.common.Event
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

internal fun MetricEvent.toTelemetryEvent(): Event? =
    when (metricName) {
        DirectionsMetrics.ROUTE_RETRIEVAL,
        NavigationMetrics.ARRIVE,
        NavigationMetrics.CANCEL_SESSION,
        NavigationMetrics.DEPART,
        NavigationMetrics.REROUTE,
        NavigationMetrics.FEEDBACK,
        NavigationMetrics.INITIAL_GPS,
        NavigationMetrics.FASTER_ROUTE,
        NavigationMetrics.CUSTOM_EVENT,
        NavigationMetrics.FREE_DRIVE -> Event(this.toValue(), null)
        else -> null
    }
