package com.mapbox.services.android.navigation.v5.utils.extensions

import com.mapbox.android.telemetry.Event
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.DirectionsMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.MetricEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetrics

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
