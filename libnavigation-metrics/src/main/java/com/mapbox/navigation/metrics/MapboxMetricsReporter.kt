package com.mapbox.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.utils.thread.WorkThreadHandler

object MapboxMetricsReporter : MetricsReporter {

    private val gson = Gson()
    private lateinit var mapboxTelemetry: MapboxTelemetry
    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var threadWorker = WorkThreadHandler("MapboxMetricsReporter")

    @JvmStatic
    fun init(
        context: Context,
        accessToken: String,
        userAgent: String
    ) {
        mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        mapboxTelemetry.enable()
    }

    // For test purposes only
    internal fun init(
        mapboxTelemetry: MapboxTelemetry,
        threadWorker: WorkThreadHandler
    ) {
        this.mapboxTelemetry = mapboxTelemetry
        this.threadWorker = threadWorker
        mapboxTelemetry.enable()
    }

    @JvmStatic
    fun disable() {
        mapboxTelemetry.disable()
    }

    fun addAppTurnstileEvent(event: AppUserTurnstile) {
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onMetricUpdated(NavigationMetrics.APP_USER_TURNSTILE, gson.toJson(event))
        }
    }

    override fun addEvent(metricEvent: MetricEvent) {
        metricEvent.toTelemetryEvent()?.let {
            mapboxTelemetry.push(it)
        }

        threadWorker.post {
            metricsObserver?.onMetricUpdated(metricEvent.metric, metricEvent.toJson(gson))
        }
    }

    override fun setMetricsObserver(metricsObserver: MetricsObserver) {
        this.metricsObserver = metricsObserver
    }
}
