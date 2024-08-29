package com.mapbox.navigation.metrics

import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsServiceError
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.EventsServiceObserver
import com.mapbox.common.SdkInformation
import com.mapbox.common.TelemetryService
import com.mapbox.common.TurnstileEvent
import com.mapbox.navigation.base.internal.metric.MetricEventInternal
import com.mapbox.navigation.base.internal.metric.extractEventsNames
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsObserver
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Default implementation of [MetricsReporter] interface.
 */
object MapboxMetricsReporter : MetricsReporter {
    private const val LOG_CATEGORY = "MapboxMetricsReporter"

    private val gson = Gson()
    private lateinit var eventsService: EventsServiceInterface
    private lateinit var telemetryService: TelemetryService

    @Volatile
    private var isTelemetryInitialized = false

    @Volatile
    private var metricsObserver: MetricsObserver? = null
    private var ioJobController = InternalJobControlFactory.createIOScopeJobControl()

    private val eventsServiceObserver =
        object : EventsServiceObserver {
            override fun didEncounterError(error: EventsServiceError, events: Value) {
                ifTelemetryIsRunning {
                    logE(LOG_CATEGORY) {
                        "EventsService failure: $error for events ${events.extractEventsNames()}"
                    }
                }
            }

            override fun didSendEvents(events: Value) {
                ifTelemetryIsRunning {
                    logD(LOG_CATEGORY) {
                        "Events has been sent ${events.extractEventsNames()}"
                    }
                }
            }
        }

    /**
     * Initialize [EventsServiceInterface] and [TelemetryService] that need to send event to
     * Mapbox Telemetry server.
     *
     * @param sdkInformation SdkInformation to be used for events service
     */
    @JvmStatic
    fun init(sdkInformation: SdkInformation) {
        isTelemetryInitialized = true
        val eventsServerOptions = EventsServerOptions(sdkInformation, null)
        eventsService = EventsServiceProvider.provideEventsService(eventsServerOptions)
        telemetryService = TelemetryServiceProvider.provideTelemetryService()
        eventsService.registerObserver(eventsServiceObserver)
    }

    /**
     * The method disables navigation telemetry, removes metrics observer and stops background
     * thread used for events dispatching.
     */
    @JvmStatic
    fun disable() {
        isTelemetryInitialized = false
        removeObserver()
        eventsService.unregisterObserver(eventsServiceObserver)
        ioJobController.job.cancelChildren()
    }

    /**
     * Adds an event to the metrics reporter when this event occurs.
     */
    override fun addEvent(metricEvent: MetricEvent) {
        ifTelemetryIsRunning {
            if (metricEvent !is MetricEventInternal) {
                logW(
                    "metricEvent must inherited from MetricEventInternal to be sent",
                    LOG_CATEGORY,
                )
                return
            }
            eventsService.sendEvent(
                Event(EventPriority.QUEUED, metricEvent.toValue(), null),
            ) { result ->
                result.onError {
                    logE("Failed to send event ${metricEvent.metricName}: $it", LOG_CATEGORY)
                }
            }

            ioJobController.scope.launch {
                metricsObserver?.onMetricUpdated(metricEvent.metricName, metricEvent.toJson(gson))
            }
        }
    }

    /**
     * Send [TurnstileEvent] event.
     */
    override fun sendTurnstileEvent(turnstileEvent: TurnstileEvent) {
        ifTelemetryIsRunning {
            eventsService.sendTurnstileEvent(turnstileEvent) { result ->
                result.onError {
                    logE("Failed to send Turnstile event: $it", LOG_CATEGORY)
                }
            }
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

    private inline fun ifTelemetryIsRunning(func: () -> Unit) {
        if (isTelemetryInitialized && TelemetryUtilsDelegate.getEventsCollectionState()) {
            func.invoke()
        } else {
            logD(
                "Navigation Telemetry is disabled",
                LOG_CATEGORY,
            )
        }
    }
}
