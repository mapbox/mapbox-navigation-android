package com.mapbox.navigation.core.internal.telemetry.standalone

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsService
import com.mapbox.common.EventsServiceInterface
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE

internal object EventsServiceProvider {
    fun provideEventsService(eventsServerOptions: EventsServerOptions): EventsServiceInterface =
        EventsService.getOrCreate(eventsServerOptions)
}

/**
 * Telemetry implementation that does not require [MapboxNavigation] object.
 * Should be replaced by native telemetry when NN decouples it from the Navigator
 * https://mapbox.atlassian.net/browse/NN-4149
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface StandaloneNavigationTelemetry {

    fun sendEvent(event: StandaloneTelemetryEvent)

    companion object {

        @Volatile
        private lateinit var INSTANCE: StandaloneNavigationTelemetry

        @Synchronized
        fun getOrCreate(): StandaloneNavigationTelemetry {
            if (!::INSTANCE.isInitialized) {
                val eventsServerOptions = EventsServerOptions(
                    SdkInfoProvider.sdkInformation(),
                    null,
                )

                INSTANCE = StandaloneNavigationTelemetryImpl(
                    EventsServiceProvider.provideEventsService(eventsServerOptions),
                )
            }
            return INSTANCE
        }

        @VisibleForTesting
        @JvmSynthetic
        @Synchronized
        internal fun reinitializeForTests() {
            if (::INSTANCE.isInitialized) {
                val eventsServerOptions = EventsServerOptions(
                    SdkInfoProvider.sdkInformation(),
                    null,
                )

                INSTANCE = StandaloneNavigationTelemetryImpl(
                    EventsServiceProvider.provideEventsService(eventsServerOptions),
                )
            }
        }
    }
}

internal class StandaloneNavigationTelemetryImpl(
    private val eventsService: EventsServiceInterface,
) : StandaloneNavigationTelemetry {

    override fun sendEvent(event: StandaloneTelemetryEvent) {
        if (!TelemetryUtilsDelegate.getEventsCollectionState()) {
            logD(LOG_CATEGORY) {
                "Skipped event send, events collection disabled"
            }
            return
        }

        eventsService.sendEvent(
            Event(EventPriority.IMMEDIATE, event.toValue(), null),
        ) { result ->
            result.onValue {
                logD(LOG_CATEGORY) {
                    "Event has been sent: ${event.metricName}"
                }
            }.onError { error ->
                logE(LOG_CATEGORY) {
                    "EventsService failure: $error. Event: ${event.metricName}"
                }
            }
        }
    }

    private companion object {
        const val LOG_CATEGORY = "StandaloneNavigationTelemetry"
    }
}
