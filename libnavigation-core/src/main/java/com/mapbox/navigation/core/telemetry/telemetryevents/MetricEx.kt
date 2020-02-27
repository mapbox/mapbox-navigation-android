package com.mapbox.navigation.core.telemetry.telemetryevents

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.metrics.internal.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.metrics.internal.utils.extentions.DirectionsMetrics
import com.mapbox.navigation.metrics.internal.utils.extentions.MetricEvent
import com.mapbox.navigation.metrics.internal.utils.extentions.NavigationMetrics

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
