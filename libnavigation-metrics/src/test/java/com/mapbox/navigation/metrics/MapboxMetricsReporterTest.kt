package com.mapbox.navigation.metrics

import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsService
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.EventsServiceObserver
import com.mapbox.common.EventsServiceResponseCallback
import com.mapbox.common.TurnstileEvent
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
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

    @Test
    fun telemetryDisabledWhenReporterDisable() {
        mockkObject(TelemetryUtilsDelegate) {
            every { TelemetryUtilsDelegate.setEventsCollectionState(any()) } just runs
            val eventService = initMetricsReporterWithTelemetry()
            every { eventService.unregisterObserver(any()) } just runs

            MapboxMetricsReporter.disable()

            verify(exactly = 1) { TelemetryUtilsDelegate.setEventsCollectionState(false) }
            verify(exactly = 1) { eventService.unregisterObserver(any()) }
        }
    }

    @Test
    fun telemetryPushCalledWhenAddValidEvent() = coroutineRule.runBlockingTest {
        mockkObject(InternalJobControlFactory) {
            val eventService = initMetricsReporterWithTelemetry()
            val metricEvent = StubNavigationEvent(NavigationMetrics.ARRIVE)
            val event = metricEvent.toTelemetryEvent()
            val slotEvent = slot<Event>()
            every { eventService.sendEvent(capture(slotEvent), any()) } just runs

            MapboxMetricsReporter.addEvent(metricEvent)

            assertTrue(slotEvent.isCaptured)
            assertEquals(EventPriority.IMMEDIATE, slotEvent.captured.priority)
            assertEquals(metricEvent.toValue(), slotEvent.captured.attributes)
            assertEquals(null, slotEvent.captured.deferredOptions)
        }
    }

    @Test
    @Ignore("logging toggle is not supported yet")
    fun telemetryCallsUpdateDebugLoggingEnabledWhenToggleLoggingIsTrue() {
        val eventService = initMetricsReporterWithTelemetry()
        val isDebugLoggingEnabled = true
        MapboxMetricsReporter.toggleLogging(isDebugLoggingEnabled)

//        verify { eventService.updateDebugLoggingEnabled(true) }
    }

    @Test
    @Ignore("logging toggle is not supported yet")
    fun telemetryCallsUpdateDebugLoggingEnabledWhenToggleLoggingIsFalse() {
        val eventService = initMetricsReporterWithTelemetry()
        val isDebugLoggingEnabled = false
        MapboxMetricsReporter.toggleLogging(isDebugLoggingEnabled)

//        verify { eventService.updateDebugLoggingEnabled(false) }
    }

    private fun initMetricsReporterWithTelemetry(): EventsServiceInterface {
        val eventsService: EventsServiceInterface = mockk(relaxUnitFun = true)
        MapboxMetricsReporter.init(eventsService, InternalJobControlFactory)
        return eventsService
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
    ) : Event(Value.nullValue(), null), MetricEvent {

        override fun toJson(gson: Gson): String = gson.toJson(this)

        override fun toValue(): Value = Value.nullValue()
    }
}
