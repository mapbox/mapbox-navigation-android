package com.mapbox.navigation.metrics.extensions

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent

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
        NavigationMetrics.FREE_DRIVE -> this as Event
        NavigationMetrics.APP_USER_TURNSTILE -> (this as NavigationAppUserTurnstileEvent).event
        else -> null
    }
