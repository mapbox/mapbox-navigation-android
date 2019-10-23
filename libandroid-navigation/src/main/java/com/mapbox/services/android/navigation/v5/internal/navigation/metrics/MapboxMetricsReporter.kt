package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.metrics.DirectionsMetrics
import com.mapbox.navigation.metrics.MetricsObserver
import com.mapbox.navigation.metrics.NavigationMetrics
import com.mapbox.services.android.navigation.v5.internal.navigation.InitialGpsEvent
import com.mapbox.services.android.navigation.v5.internal.navigation.RouteRetrievalEvent
import com.mapbox.services.android.navigation.v5.utils.extensions.fromJson
import com.mapbox.services.android.navigation.v5.utils.thread.WorkThreadHandler

internal object MapboxMetricsReporter : DirectionsMetrics, NavigationMetrics {

    private lateinit var gson: Gson
    private lateinit var mapboxTelemetry: MapboxTelemetry
    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private val threadWorker = WorkThreadHandler("MapboxMetricsReporter")

    fun init(
        context: Context,
        accessToken: String,
        userAgent: String,
        gson: Gson
    ) {
        this.mapboxTelemetry = MapboxTelemetry(context, accessToken, userAgent)
        this.mapboxTelemetry.enable()
        this.gson = gson
    }

    fun disable() {
        mapboxTelemetry.disable()
    }

    override fun toggleLogging(isDebugLoggingEnabled: Boolean) {
        mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled)
    }

    override fun arriveEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<NavigationArriveEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onJsonStringMetricUpdated(eventName, eventJsonString)
        }
    }

    override fun cancelEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<NavigationCancelEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun departEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<NavigationDepartEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun rerouteEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<NavigationRerouteEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun feedbackEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<NavigationFeedbackEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun routeRetrievalEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<RouteRetrievalEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun sendInitialGpsEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<InitialGpsEvent>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    override fun sendTurnstileEvent(eventName: String, eventJsonString: String) {
        val event = gson.fromJson<AppUserTurnstile>(eventJsonString)
        mapboxTelemetry.push(event)

        threadWorker.post {
            metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        }
    }

    fun setMetricsObserver(observer: MetricsObserver?) {
        MapboxMetricsReporter.metricsObserver.set(observer)
    }
}
