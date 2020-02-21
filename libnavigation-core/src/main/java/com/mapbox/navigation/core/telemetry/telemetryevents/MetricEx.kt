package com.mapbox.navigation.core.telemetry.telemetryevents

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics

fun MetricEvent.toTelemetryEvent(): Event? =
    when (metricName) {
        DirectionsMetrics.ROUTE_RETRIEVAL,
        NavigationMetrics.ARRIVE,
        NavigationMetrics.CANCEL_SESSION,
        NavigationMetrics.DEPART,
        NavigationMetrics.REROUTE,
        NavigationMetrics.FEEDBACK,
        NavigationMetrics.INITIAL_GPS -> this as Event
        NavigationMetrics.APP_USER_TURNSTILE -> (this as NavigationAppUserTurnstileEvent).event
        else -> null
    }
