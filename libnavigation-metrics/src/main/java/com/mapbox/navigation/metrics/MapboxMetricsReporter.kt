package com.mapbox.navigation.metrics

import android.content.Context
import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsServiceError
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.EventsServiceObserver
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Default implementation of [MetricsReporter] interface.
 */
object MapboxMetricsReporter : MetricsReporter {
    private const val LOG_CATEGORY = "MapboxMetricsReporter"

    private val gson = Gson()
    private lateinit var eventsService: EventsServiceInterface

    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var ioJobController = InternalJobControlFactory.createIOScopeJobControl()

    private val eventsServiceObserver by lazy {
        object : EventsServiceObserver {
            override fun didEncounterError(error: EventsServiceError, events: Value) {
                logE("EventsService failure: $error", LOG_CATEGORY)
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
        eventsService = EventsServiceProvider.provideEventsService(
            EventsServerOptions(accessToken, userAgent, null)
        )

        if (!TelemetryUtilsDelegate.getEventsCollectionState()) {
            TelemetryUtilsDelegate.setEventsCollectionState(true)
        }
    }

    // For test purposes only
    internal fun init(
        eventsService: EventsServiceInterface,
        jobControlFactory: InternalJobControlFactory
    ) {
        this.eventsService = eventsService
        ioJobController = jobControlFactory.createIOScopeJobControl()
    }

    /**
     * Toggle whether or not you'd like to log [mapboxTelemetry] events.
     *
     * @param isDebugLoggingEnabled true to enable logging, false to disable logging
     */
    @JvmStatic
    fun toggleLogging(isDebugLoggingEnabled: Boolean) {
        // todo need support on core side
        // mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled)
    }

    /**
     * Disables metrics reporting and ends [mapboxTelemetry] session.
     * This method also removes metrics observer and stops background thread used for
     * events dispatching.
     */
    @JvmStatic
    fun disable() {
        removeObserver()
        eventsService.unregisterObserver(eventsServiceObserver)
        TelemetryUtilsDelegate.setEventsCollectionState(false)
        ioJobController.job.cancelChildren()
    }

    /**
     * Adds an event to the metrics reporter when this event occurs.
     */
    override fun addEvent(metricEvent: MetricEvent) {
        eventsService.sendEvent(
            Event(EventPriority.IMMEDIATE, metricEvent.toValue(), null)
        ) { error ->
            error?.let {
                logE("Failed to send event: $error", LOG_CATEGORY)
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
