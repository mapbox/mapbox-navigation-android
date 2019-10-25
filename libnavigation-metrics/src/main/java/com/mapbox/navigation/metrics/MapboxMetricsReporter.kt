package com.mapbox.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.utils.thread.WorkThreadHandler

object MapboxMetricsReporter : MetricsReporter {

    private val gson = Gson()
    private lateinit var mapboxTelemetry: MapboxTelemetry
    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private val threadWorker = WorkThreadHandler("MapboxMetricsReporter")

    fun init(
        context: Context,
        accessToken: String,
        userAgent: String
    ) {
        mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        mapboxTelemetry.enable()
    }

    fun disable() {
        mapboxTelemetry.disable()
    }

    override fun addEvent(@Metric metric: String, event: Event) {
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onMetricUpdated(metric, gson.toJson(event))
        }
    }

    override fun setMetricsObserver(metricsObserver: MetricsObserver) {
        this.metricsObserver = metricsObserver
    }
}
