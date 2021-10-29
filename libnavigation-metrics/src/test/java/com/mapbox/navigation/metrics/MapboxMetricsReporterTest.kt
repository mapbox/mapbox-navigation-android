package com.mapbox.navigation.metrics

import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.bindgen.Value
import com.mapbox.common.*
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements


@Implements(EventsService::class)
object ShadowEventsService {
    @Implementation
    open fun setEventsCollectionState(
        enableCollection: Boolean,
        callback: EventsServiceResponseCallback?
    ) {}


    @Implementation
    open fun getEventsCollectionState(): Boolean {
        return true
    }
}

@ExperimentalCoroutinesApi
@Config(shadows = [ShadowEventsService::class])
@RunWith(RobolectricTestRunner::class)
class MapboxMetricsReporterTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private var eventsServiceMock = object : EventsServiceInterface {
        override fun registerObserver(observer: EventsServiceObserver) {}

        override fun unregisterObserver(observer: EventsServiceObserver) {}

        override fun sendTurnstileEvent(
            turnstileEvent: TurnstileEvent,
            callback: EventsServiceResponseCallback?
        ) {}

        override fun sendEvent(
            event: com.mapbox.common.Event,
            callback: EventsServiceResponseCallback?
        ) {}

        override fun pauseEventsCollection() {}

        override fun resumeEventsCollection() {}

    }

    @Test
    fun telemetryEnabledWhenReporterInit() {
        val mapboxTelemetry = mockk<MapboxTelemetry>(relaxed = true)
        val eventsService: EventsServiceInterface = mockk()

        MapboxMetricsReporter.init(mapboxTelemetry, eventsServiceMock, InternalJobControlFactory)
        verify { mapboxTelemetry.enable() }
    }

    @Test
    fun telemetryDisabledWhenReporterDisable() {
        val mapboxTelemetry = initMetricsReporterWithTelemetry()

        MapboxMetricsReporter.disable()

        verify { mapboxTelemetry.disable() }
    }

    @Test
    fun telemetryPushCalledWhenAddValidEvent() = coroutineRule.runBlockingTest {
        mockkObject(InternalJobControlFactory)
        mockIOScopeAndRootJob()
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val metricEvent = StubNavigationEvent(NavigationMetrics.ARRIVE)
        val event = metricEvent.toTelemetryEvent()

        MapboxMetricsReporter.addEvent(metricEvent)

        verify { mapboxTelemetry.push(event) }
        unmockkObject(InternalJobControlFactory)
    }

    @Test
    fun telemetryPushCalledWhenAddInvalidEvent() = coroutineRule.runBlockingTest {
        mockkObject(InternalJobControlFactory)
        mockIOScopeAndRootJob()
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val metricEvent = StubNavigationEvent("some_event")
        val event = metricEvent.toTelemetryEvent()

        MapboxMetricsReporter.addEvent(metricEvent)

        verify(exactly = 0) { mapboxTelemetry.push(event) }
        unmockkObject(InternalJobControlFactory)
    }

    @Test
    fun telemetryCallsUpdateDebugLoggingEnabledWhenToggleLoggingIsTrue() {
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val isDebugLoggingEnabled = true
        MapboxMetricsReporter.toggleLogging(isDebugLoggingEnabled)

        verify { mapboxTelemetry.updateDebugLoggingEnabled(true) }
    }

    @Test
    fun telemetryCallsUpdateDebugLoggingEnabledWhenToggleLoggingIsFalse() {
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val isDebugLoggingEnabled = false
        MapboxMetricsReporter.toggleLogging(isDebugLoggingEnabled)

        verify { mapboxTelemetry.updateDebugLoggingEnabled(false) }
    }

    private fun initMetricsReporterWithTelemetry(): MapboxTelemetry {
        val mapboxTelemetry = mockk<MapboxTelemetry>(relaxed = true)
        val eventsService: EventsServiceInterface = mockk()

        mockkStatic(EventsService::class)
        every { EventsService.setEventsCollectionState(any(), any()) } returns

        MapboxMetricsReporter.init(mapboxTelemetry, eventsServiceMock, InternalJobControlFactory)
        return mapboxTelemetry
    }

    private fun mockIOScopeAndRootJob() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        every {
            InternalJobControlFactory.createIOScopeJobControl()
        } returns JobControl(parentJob, testScope)
    }

    private class StubNavigationEvent(
        override val metricName: String
    ) : Event(), MetricEvent {

        override fun writeToParcel(dest: Parcel?, flags: Int) {}

        override fun describeContents(): Int = 0

        override fun toJson(gson: Gson): String = gson.toJson(this)

        override fun toValue(): Value = Value.nullValue()
    }
}
