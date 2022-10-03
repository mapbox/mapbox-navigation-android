package com.mapbox.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService
import com.mapbox.common.EventsServiceError
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.EventsServiceObserver
import com.mapbox.common.MapboxCommonLogger
import com.mapbox.common.TelemetryUtils
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Default implementation of [MetricsReporter] interface.
 */
object MapboxMetricsReporter : MetricsReporter {
    private const val TAG = "MBMetricsReporter"

    private val gson = Gson()
    private lateinit var mapboxTelemetry: MapboxTelemetry
    private lateinit var eventsService: EventsServiceInterface

    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var ioJobController = InternalJobControlFactory.createIOScopeJobControl()

    private val eventsServiceObserver by lazy {
        object : EventsServiceObserver {
            override fun didEncounterError(error: EventsServiceError, events: Value) {
                MapboxCommonLogger.logE(TAG, "EventsService failure: $error")
            }

            override fun didSendEvents(events: Value) {}
        }
    }

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
        val oldEventsTokenResId = context.resources.getIdentifier("old_mapbox_events_access_token", "string", context.packageName)
        val oldEventsToken =
            if (oldEventsTokenResId != 0) context.getString(oldEventsTokenResId) else accessToken

        mapboxTelemetry = MapboxTelemetry(context, oldEventsToken, userAgent)
        mapboxTelemetry.enable()
        val eventsServiceOptions = EventsServerOptions(accessToken, userAgent, null)
        eventsService = EventsService.getOrCreate(eventsServiceOptions.overrideIfNeeded(context))

        if (!TelemetryUtils.getEventsCollectionState()) {
            TelemetryUtils.setEventsCollectionState(true, {})
        }
    }

    // For test purposes only
    internal fun init(
        mapboxTelemetry: MapboxTelemetry,
        eventsService: EventsServiceInterface,
        jobControlFactory: InternalJobControlFactory
    ) {
        this.mapboxTelemetry = mapboxTelemetry
        this.eventsService = eventsService
        ioJobController = jobControlFactory.createIOScopeJobControl()
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
        eventsService.unregisterObserver(eventsServiceObserver)
        TelemetryUtils.setEventsCollectionState(false, {})
        ioJobController.job.cancelChildren()
    }

    /**
     * Adds an event to the metrics reporter when this event occurs.
     */
    override fun addEvent(metricEvent: MetricEvent) {
        metricEvent.toTelemetryEvent()?.let {
            mapboxTelemetry.push(it)
        }

        eventsService.sendEvent(Event(EventPriority.IMMEDIATE, metricEvent.toValue(), null)) { error ->
            error?.let {
                MapboxCommonLogger.logE(TAG, "Failed to send event: $error")
            }
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
}

/**
 * Read url and token from resources if they are available
 */
fun EventsServerOptions.overrideIfNeeded(context: Context): EventsServerOptions {
    val endpointId = context.resources.getIdentifier(
        "mapbox_events_url",
        "string",
        context.packageName
    )
    val tokenId = context.resources.getIdentifier(
        "mapbox_events_access_token",
        "string",
        context.packageName
    )
    return if (endpointId != 0 || tokenId != 0) {
        EventsServerOptions(
            if (tokenId != 0) context.getString(tokenId) else token,
            userAgentFragment,
            null
        )
    } else this
}
