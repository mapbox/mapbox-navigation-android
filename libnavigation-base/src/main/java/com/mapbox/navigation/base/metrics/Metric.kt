package com.mapbox.navigation.base.metrics

import androidx.annotation.StringDef

@StringDef(
    DirectionsMetrics.ROUTE_RETRIEVAL,
    NavigationMetrics.ARRIVE,
    NavigationMetrics.CANCEL_SESSION,
    NavigationMetrics.DEPART,
    NavigationMetrics.REROUTE,
    NavigationMetrics.FEEDBACK,
    NavigationMetrics.INITIAL_GPS,
    NavigationMetrics.APP_USER_TURNSTILE
)
annotation class Metric
