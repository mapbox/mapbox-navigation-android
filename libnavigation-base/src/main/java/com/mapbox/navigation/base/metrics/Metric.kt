package com.mapbox.navigation.metrics

import androidx.annotation.StringDef

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
