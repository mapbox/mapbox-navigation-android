package com.mapbox.navigation.base.internal.metrics

import androidx.annotation.StringDef
import com.google.gson.Gson

interface MetricEvent {

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

    @Metric
    val metric: String

    fun toJson(gson: Gson): String
}
