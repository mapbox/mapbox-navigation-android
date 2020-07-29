package com.mapbox.navigation.metrics

import android.content.Context
import android.location.Location
import android.os.Build
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import java.lang.reflect.Type
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Default implementation of [MetricsReporter] interface.
 */
object MapboxMetricsReporter : MetricsReporter {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Location::class.java, LocationSerializer())
        .create()

    private lateinit var mapboxTelemetry: MapboxTelemetry

    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var ioJobController: JobControl = ThreadController.getIOScopeAndRootJob()

    /**
     * Initialize [mapboxTelemetry] that need to send event to Mapbox Telemetry server.
     *
     * @param context Android context
     * @param accessToken Mapbox access token
     * @param userAgent Use agent indicate source of metrics
     */
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
        threadController: ThreadController
    ) {
        this.mapboxTelemetry = mapboxTelemetry
        this.ioJobController = threadController.getMainScopeAndRootJob()
        mapboxTelemetry.enable()
    }

    /**
     * Toggle whether or not you'd like to log [mapboxTelemetry] events.
     *
     * @param isDebugLoggingEnabled true to enable logging, false to disable logging
     */
    @JvmStatic
    fun toggleLogging(isDebugLoggingEnabled: Boolean) {
        mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled)
    }

    /**
     * Disables metrics reporting and ends [mapboxTelemetry] session.
     * This method also removes metrics observer and stops background thread used for
     * events dispatching.
     */
    @JvmStatic
    fun disable() {
        removeObserver()
        mapboxTelemetry.disable()
        ioJobController.job.cancelChildren()
    }

    /**
     * Adds an event to the metrics reporter when this event occurs.
     */
    override fun addEvent(metricEvent: MetricEvent) {
        metricEvent.toTelemetryEvent()?.let {
            mapboxTelemetry.push(it)
        }

        ioJobController.scope.launch {
            metricsObserver?.onMetricUpdated(metricEvent.metricName, metricEvent.toJson(gson))
        }
    }

    /**
     * Adds a [MetricsObserver] that will be triggered when a metric event is handled.
     */
    override fun setMetricsObserver(metricsObserver: MetricsObserver) {
        this.metricsObserver = metricsObserver
    }

    /**
     * Remove the [MetricsObserver].
     */
    override fun removeObserver() {
        this.metricsObserver = null
    }

    private class LocationSerializer : JsonSerializer<Location> {
        override fun serialize(
            location: Location,
            type: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonObject().apply {
                addProperty(LATITUDE, location.latitude)
                addProperty(LONGITUDE, location.longitude)
                addProperty(SPEED, location.speed)
                addProperty(COURSE, location.bearing)
                addProperty(ALTITUDE, location.altitude)
                addProperty(TIMESTAMP, location.time)
                addProperty(HORIZONTAL_ACCURACY, location.accuracy)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addProperty(VERTICAL_ACCURACY, location.verticalAccuracyMeters)
                }
            }
        }
    }

    private const val LATITUDE = "lat"
    private const val LONGITUDE = "lng"
    private const val SPEED = "speed"
    private const val COURSE = "course"
    private const val ALTITUDE = "altitude"
    private const val TIMESTAMP = "timestamp"
    private const val HORIZONTAL_ACCURACY = "horizontalAccuracy"
    private const val VERTICAL_ACCURACY = "verticalAccuracy"
}
