package com.mapbox.navigation.metrics.extensions

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

fun MetricEvent.toTelemetryEvent(): Event? =
    when (metric) {
        DirectionsMetrics.ROUTE_RETRIEVAL,
        NavigationMetrics.ARRIVE,
        NavigationMetrics.CANCEL_SESSION,
        NavigationMetrics.DEPART,
        NavigationMetrics.REROUTE,
        NavigationMetrics.FEEDBACK,
        NavigationMetrics.INITIAL_GPS,
        NavigationMetrics.APP_USER_TURNSTILE -> this as Event
        else -> null
    }
