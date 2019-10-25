package com.mapbox.navigation.base.metrics

import androidx.annotation.StringDef
import com.mapbox.navigation.base.metrics.DirectionsMetrics
import com.mapbox.navigation.base.metrics.NavigationMetrics

@StringDef(
    DirectionsMetrics.ROUTE_RETRIEVAL,
    NavigationMetrics.ARRIVE,
    NavigationMetrics.CANCEL_SESSION,
    NavigationMetrics.DEPART,
    NavigationMetrics.REROUTE,
    NavigationMetrics.FEEDBACK,
    NavigationMetrics.INITIAL_GPS,
    NavigationMetrics.APP_USER_TURNSTILE,
    NavigationMetrics.PERFORMANCE
)
annotation class Metric
