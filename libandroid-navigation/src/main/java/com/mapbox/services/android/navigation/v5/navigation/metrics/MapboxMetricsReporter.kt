package com.mapbox.services.android.navigation.v5.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.utils.thread.WorkThreadHandler
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.MetricsReporter
import com.mapbox.services.android.navigation.v5.internal.utils.extensions.toTelemetryEvent

/**
 * Default implementation of [MetricsReporter] interface.
 *
 * @since 0.43.0
 */
object MapboxMetricsReporter :
    MetricsReporter {

    private val gson = Gson()
    private lateinit var mapboxTelemetry: MapboxTelemetry
    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var threadWorker = WorkThreadHandler("MapboxMetricsReporter")

    /**
     * Initialize [mapboxTelemetry] that need to send event to Mapbox Telemetry server.
     *
     * @param context Android context
     * @param accessToken Mapbox access token
     * @param userAgent Use agent indicate source of metrics
     * @since 0.43.0
     */
    @JvmStatic
    fun init(
        context: Context,
        accessToken: String,
        userAgent: String
    ) {
        mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        mapboxTelemetry.enable()
        threadWorker.start()
    }

    // For test purposes only
    internal fun init(
        mapboxTelemetry: MapboxTelemetry,
        threadWorker: WorkThreadHandler
    ) {
        this.mapboxTelemetry = mapboxTelemetry
        this.threadWorker = threadWorker
        this.threadWorker.start()
        mapboxTelemetry.enable()
    }

    /**
     * Disable [mapboxTelemetry] to finish telemetry session when it needed.
     *
     * @since 0.43.0
     */
    @JvmStatic
    fun disable() {
        mapboxTelemetry.disable()
        threadWorker.stop()
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
