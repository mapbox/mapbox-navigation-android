package com.mapbox.navigation.metrics

import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.internal.metric.MetricEventInternal
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxMetricsReporterTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val logger = mockk<LoggerFrontend>(relaxUnitFun = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    @Before
    fun setup() {
        mockkObject(EventsServiceProvider)
        mockkObject(TelemetryUtilsDelegate)
        mockkObject(TelemetryServiceProvider)

        every { TelemetryUtilsDelegate.setEventsCollectionState(any()) } just runs
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true
    }

    @After
    fun cleanup() {
        unmockkObject(EventsServiceProvider)
        unmockkObject(TelemetryUtilsDelegate)
        unmockkObject(TelemetryServiceProvider)
    }

    @Test
    fun onTelemetryInit() {
        val eventService = initMetricsReporterWithTelemetry()

        verify(exactly = 1) { eventService.registerObserver(any()) }
    }

    @Test
    fun `telemetry events are not processed till it becomes init`() {
        MapboxMetricsReporter.sendTurnstileEvent(mockk())
        MapboxMetricsReporter.addEvent(mockk())

        verify(exactly = 2) {
            logger.logD(
                "Navigation Telemetry is disabled",
                "MapboxMetricsReporter",
            )
        }
    }

    @Test
    fun telemetryDisabledWhenReporterDisable() {
        val eventService = initMetricsReporterWithTelemetry()

        MapboxMetricsReporter.disable()
        MapboxMetricsReporter.sendTurnstileEvent(mockk())
        MapboxMetricsReporter.addEvent(mockk())

        verify(exactly = 1) { eventService.unregisterObserver(any()) }
        verify(exactly = 0) { eventService.sendTurnstileEvent(any(), any()) }
        verify(exactly = 0) { eventService.sendEvent(any(), any()) }
    }

    @Test
    fun `events aren't sent if telemetry is disabled globally`() {
        val eventService = initMetricsReporterWithTelemetry()

        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false
        MapboxMetricsReporter.sendTurnstileEvent(mockk())
        MapboxMetricsReporter.addEvent(mockk())

        verify(exactly = 0) { eventService.sendTurnstileEvent(any(), any()) }
        verify(exactly = 0) { eventService.sendEvent(any(), any()) }
    }

    @Test
    fun telemetryPushCalledWhenAddValidEvent() = coroutineRule.runBlockingTest {
        mockkObject(InternalJobControlFactory) {
            val eventService = initMetricsReporterWithTelemetry()
            val metricEvent = StubNavigationEvent(NavigationMetrics.ARRIVE)
            val slotEvent = slot<Event>()
            every { eventService.sendEvent(capture(slotEvent), any()) } just runs

            MapboxMetricsReporter.addEvent(metricEvent)

            assertTrue(slotEvent.isCaptured)
            assertEquals(EventPriority.QUEUED, slotEvent.captured.priority)
            assertEquals(metricEvent.toValue(), slotEvent.captured.attributes)
            assertEquals(null, slotEvent.captured.deferredOptions)
        }
    }

    @Test
    fun `reporter is not sent pure MetricEvent (must be MetricEventInternal)`() {
        val eventService = initMetricsReporterWithTelemetry()
        val metricEvent = mockk<MetricEvent>()

        MapboxMetricsReporter.addEvent(metricEvent)

        verify(exactly = 0) { eventService.sendEvent(any(), any()) }
    }

    private fun initMetricsReporterWithTelemetry(): EventsServiceInterface {
        val eventsService: EventsServiceInterface = mockk(relaxUnitFun = true)
        every { EventsServiceProvider.provideEventsService(any()) } returns eventsService
        every {
            TelemetryServiceProvider.provideTelemetryService()
        } returns mockk(relaxUnitFun = true)
        MapboxMetricsReporter.init(SdkInformation("name", "2.16.0", null))
        return eventsService
    }

    private class StubNavigationEvent(
        override val metricName: String,
    ) : Event(Value.nullValue(), null), MetricEventInternal {

        override fun toJson(gson: Gson): String = gson.toJson(this)

        override fun toValue(): Value = Value.nullValue()
    }
}
