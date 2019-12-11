package com.mapbox.navigation.base.metrics

import androidx.annotation.StringDef
import com.google.gson.Gson
import com.mapbox.navigation.base.internal.metrics.DirectionsMetrics
import com.mapbox.navigation.base.internal.metrics.NavigationMetrics

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
    val metricName: String

    fun toJson(gson: Gson): String
}

object NavigationMetrics {
    const val ARRIVE = "navigation.arrive"
    const val CANCEL_SESSION = "navigation.cancel"
    const val DEPART = "navigation.depart"
    const val REROUTE = "navigation.reroute"
    const val FEEDBACK = "navigation.feedback"
    const val INITIAL_GPS = "initial_gps_event"
    const val APP_USER_TURNSTILE = "appUserTurnstile"
}

object DirectionsMetrics {
    const val ROUTE_RETRIEVAL = "route_retrieval_event"
}
