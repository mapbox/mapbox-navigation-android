package com.mapbox.services.android.navigation.v5.navigation.metrics

import androidx.annotation.StringDef
import com.google.gson.Gson
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.DirectionsMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.NavigationMetrics

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
