package com.mapbox.navigation.core.internal.telemetry.standalone

import com.mapbox.bindgen.Value
import com.mapbox.common.Event
import com.mapbox.common.EventPriority
import com.mapbox.common.EventsServerOptions
import com.mapbox.common.EventsServiceInterface
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StandaloneNavigationTelemetryTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var eventsService: EventsServiceInterface

    @Before
    fun setUp() {
        eventsService = mockk(relaxed = true)
        mockkObject(EventsServiceProvider)
        every { EventsServiceProvider.provideEventsService(any()) } returns eventsService

        mockkObject(SdkInfoProvider)
        every { SdkInfoProvider.sdkInformation() } returns TEST_SDK_INFO

        mockkObject(TelemetryUtilsDelegate)
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true

        StandaloneNavigationTelemetry.reinitializeForTests()
    }

    @After
    fun tearDown() {
        unmockkObject(EventsServiceProvider)
        unmockkObject(SdkInfoProvider)
        unmockkObject(TelemetryUtilsDelegate)
    }

    @Test
    fun `EventsService should be created with current SdkInformation`() {
        val optionsSlot = slot<EventsServerOptions>()
        every {
            EventsServiceProvider.provideEventsService(capture(optionsSlot))
        } returns eventsService

        StandaloneNavigationTelemetry.getOrCreate()
        assertEquals(TEST_SDK_INFO, optionsSlot.captured.sdkInformation)
    }

    @Test
    fun `getOrCreate should return the same instance`() {
        val instance1 = StandaloneNavigationTelemetry.getOrCreate()
        val instance2 = StandaloneNavigationTelemetry.getOrCreate()
        assertSame(instance1, instance2)
    }

    @Test
    fun `sendEvent should pass event to the events service`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true

        val eventSlot = slot<Event>()
        every { eventsService.sendEvent(capture(eventSlot), any()) } just Runs

        val eventValue = mockk<Value>(relaxed = true)
        val event: StandaloneTelemetryEvent = mockk(relaxed = true) {
            every { toValue() } returns eventValue
        }

        StandaloneNavigationTelemetry.getOrCreate().sendEvent(event)

        verify(exactly = 1) {
            eventsService.sendEvent(any(), any())
        }

        assertEquals(EventPriority.IMMEDIATE, eventSlot.captured.priority)
        assertEquals(eventValue, eventSlot.captured.attributes)
    }

    @Test
    fun `sendEvent must not pass event to the events service if events collection is disabled`() {
        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns false
        StandaloneNavigationTelemetry.getOrCreate().sendEvent(mockk())
        verify(exactly = 0) {
            eventsService.sendEvent(any(), any())
        }
    }

    private companion object {
        val TEST_SDK_INFO = SdkInformation("test-name", "test-version", "test-package")
    }
}
