package com.mapbox.services.android.navigation.v5.internal.utils.extensions

import com.mapbox.android.telemetry.Event
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.DirectionsMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationAppUserTurnstileEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetrics
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent

internal fun MetricEvent.toTelemetryEvent(): Event? =
    when (metric) {
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
