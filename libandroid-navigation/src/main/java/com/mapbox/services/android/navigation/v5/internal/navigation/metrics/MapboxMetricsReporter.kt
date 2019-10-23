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

internal object MapboxMetricsReporter: DirectionsMetrics, NavigationMetrics {

    private lateinit var gson: Gson
    private lateinit var mapboxTelemetry: MapboxTelemetry
    private var metricsObserver: MetricsObserver? = null

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
        if (::mapboxTelemetry.isInitialized) {
            mapboxTelemetry.disable()
        }
    }

    override fun toggleLogging(isDebugLoggingEnabled: Boolean) {
        mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled)
    }

    override fun arriveEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, NavigationArriveEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onJsonStringMetricUpdated(eventName, eventJsonString)
        mapboxTelemetry.push(event)
    }

    override fun cancelEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, NavigationCancelEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun departEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, NavigationDepartEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun rerouteEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, NavigationRerouteEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun feedbackEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, NavigationFeedbackEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun routeRetrievalEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, RouteRetrievalEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun sendInitialGpsEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, InitialGpsEvent::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    override fun sendTurnstileEvent(eventName: String, eventJsonString: String) {
        val event = fromGson(eventJsonString, AppUserTurnstile::class.java)
        // TODO: Move metricsObserver call to separate thread
        metricsObserver?.onStringMetricUpdated(eventName, event.toString())
        mapboxTelemetry.push(event)
    }

    fun setMetricsObserver(metricsObserver: MetricsObserver?) {
        this.metricsObserver = metricsObserver
    }

    private fun <T> fromGson(json: String, clazz: Class<T>): T =
        gson.fromJson(json, clazz)
}