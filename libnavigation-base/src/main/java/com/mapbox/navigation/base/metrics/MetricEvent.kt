package com.mapbox.navigation.base.metrics

import androidx.annotation.StringDef
import com.mapbox.navigation.base.internal.metrics.DirectionsMetrics
import com.mapbox.navigation.base.internal.metrics.NavigationMetrics
import com.mapbox.navigation.base.util.JsonMapper

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

    fun toJson(jsonMapper: JsonMapper): String
}
