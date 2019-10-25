package com.mapbox.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.services.android.navigation.v5.internal.navigation.InitialGpsEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.RouteRetrievalEvent
import com.mapbox.services.android.navigation.v5.utils.extensions.fromJson
import com.mapbox.services.android.navigation.v5.utils.thread.WorkThreadHandler

object MapboxMetricsReporter : MetricsReporter {

    private lateinit var gson: Gson
    private var mapboxTelemetry: MapboxTelemetry? = null
    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private val threadWorker = WorkThreadHandler("MapboxMetricsReporter")

    fun init(
        context: Context,
        accessToken: String,
        userAgent: String,
        gson: Gson
    ) {
        mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        mapboxTelemetry?.enable()
        MapboxMetricsReporter.gson = gson
    }

    fun disable() {
        mapboxTelemetry?.disable()
    }

    override fun addEvent(@Metric metric: String, event: Event) {
        when (metric) {
            NavigationMetrics.ARRIVE -> gson.fromJson<NavigationArriveEvent>(eventJsonString)
            NavigationMetrics.CANCEL_SESSION -> gson.fromJson<NavigationCancelEvent>(eventJsonString)
            NavigationMetrics.DEPART -> gson.fromJson<NavigationDepartEvent>(eventJsonString)
            NavigationMetrics.REROUTE -> gson.fromJson<NavigationRerouteEvent>(eventJsonString)
            NavigationMetrics.FEEDBACK -> gson.fromJson<NavigationFeedbackEvent>(eventJsonString)
            NavigationMetrics.INITIAL_GPS -> gson.fromJson<InitialGpsEvent>(eventJsonString)
            NavigationMetrics.APP_USER_TURNSTILE -> gson.fromJson<AppUserTurnstile>(eventJsonString)
            NavigationMetrics.PERFORMANCE -> gson.fromJson<NavigationPerformanceEvent>(eventJsonString)
            DirectionsMetrics.ROUTE_RETRIEVAL -> gson.fromJson<RouteRetrievalEvent>(eventJsonString)
            else -> null
        }?.let {
            mapboxTelemetry?.push(it)

            threadWorker.post {
                metricsObserver?.onMetricUpdated(metric, eventJsonString)
            }
        }
    }

    override fun setMetricsObserver(metricsObserver: MetricsObserver) {
        MapboxMetricsReporter.metricsObserver = metricsObserver
    }
}
