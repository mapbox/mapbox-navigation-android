package com.mapbox.navigation.metrics

import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.extensions.toTelemetryEvent
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxMetricsReporterTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun telemetryEnabledWhenReporterInit() {
        val mapboxTelemetry = mockk<MapboxTelemetry>(relaxed = true)

        MapboxMetricsReporter.init(mapboxTelemetry, ThreadController)

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
        mockkObject(ThreadController)
        mockIOScopeAndRootJob()
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val metricEvent = StubNavigationEvent(NavigationMetrics.ARRIVE)
        val event = metricEvent.toTelemetryEvent()

        MapboxMetricsReporter.addEvent(metricEvent)

        verify { mapboxTelemetry.push(event) }
        unmockkObject(ThreadController)
    }

    @Test
    fun telemetryPushCalledWhenAddInvalidEvent() = coroutineRule.runBlockingTest {
        mockkObject(ThreadController)
        mockIOScopeAndRootJob()
        val mapboxTelemetry = initMetricsReporterWithTelemetry()
        val metricEvent = StubNavigationEvent("some_event")
        val event = metricEvent.toTelemetryEvent()

        MapboxMetricsReporter.addEvent(metricEvent)

        verify(exactly = 0) { mapboxTelemetry.push(event) }
        unmockkObject(ThreadController)
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
        MapboxMetricsReporter.init(mapboxTelemetry, ThreadController)

        return mapboxTelemetry
    }

    private fun mockIOScopeAndRootJob() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
    }

    private class StubNavigationEvent(
        override val metricName: String
    ) : Event(), MetricEvent {

        override fun writeToParcel(dest: Parcel?, flags: Int) {}

        override fun describeContents(): Int = 0

        override fun toJson(gson: Gson): String = gson.toJson(this)
    }
}
