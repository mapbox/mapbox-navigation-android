package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.LegacyMapboxNavigationInstanceHolder
import com.mapbox.navigation.core.internal.MapboxNavigationCreateObserver
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceInstructionsPredownloadHubTest {

    private val loader = mockk<MapboxSpeechLoader>(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    @Before
    fun setUp() {
        VoiceInstructionsPredownloadHub.unregisterAll()
        mockkObject(LegacyMapboxNavigationInstanceHolder)
    }

    @After
    fun tearDown() {
        VoiceInstructionsPredownloadHub.unregisterAll()
        unmockkObject(LegacyMapboxNavigationInstanceHolder)
    }

    @Test
    fun `register first registrant`() {
        VoiceInstructionsPredownloadHub.register(loader)

        checkRegistrantIsRegistered(loader)
    }

    @Test
    fun `register new registrant`() {
        VoiceInstructionsPredownloadHub.register(loader)
        val newLoader = mockk<MapboxSpeechLoader>(relaxed = true)
        clearAllMocks(answers = false)

        VoiceInstructionsPredownloadHub.register(newLoader)

        checkRegistrantIsRegistered(newLoader)
    }

    @Test
    fun `register the same registrant`() {
        VoiceInstructionsPredownloadHub.register(loader)
        clearAllMocks(answers = false)

        VoiceInstructionsPredownloadHub.register(loader)

        checkRegistrantIsNotRegistered(loader)
    }

    @Test
    fun `register removed registrant`() {
        VoiceInstructionsPredownloadHub.register(loader)
        VoiceInstructionsPredownloadHub.unregister(loader)
        clearAllMocks(answers = false)

        VoiceInstructionsPredownloadHub.register(loader)

        checkRegistrantIsRegistered(loader)
    }

    @Test
    fun `unregister non-existent registrant`() {
        VoiceInstructionsPredownloadHub.unregister(loader)

        verify(exactly = 0) {
            LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(any())
            mapboxNavigation.unregisterRouteProgressObserver(any())
            mapboxNavigation.unregisterRoutesObserver(any())
        }
    }

    @Test
    fun `unregister existent registrant for non null current mapbox navigation`() {
        VoiceInstructionsPredownloadHub.register(loader)
        val mapboxNavigationObserver = captureMapboxNavigationObserver()
        val trigger = captureTrigger(mapboxNavigationObserver)
        every { LegacyMapboxNavigationInstanceHolder.peek() } returns mapboxNavigation
        clearAllMocks(answers = false)

        VoiceInstructionsPredownloadHub.unregister(loader)

        verify(exactly = 1) {
            LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(mapboxNavigationObserver)
            mapboxNavigation.unregisterRoutesObserver(trigger)
            mapboxNavigation.unregisterRouteProgressObserver(trigger)
        }
        assertFalse(loader in trigger.observers)
    }

    @Test
    fun `unregister existent registrant for null current mapbox navigation`() {
        VoiceInstructionsPredownloadHub.register(loader)
        val mapboxNavigationObserver = captureMapboxNavigationObserver()
        val trigger = captureTrigger(mapboxNavigationObserver)
        every { LegacyMapboxNavigationInstanceHolder.peek() } returns null

        VoiceInstructionsPredownloadHub.unregister(loader)

        verify(exactly = 1) {
            LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(mapboxNavigationObserver)
        }
        verify(exactly = 0) {
            mapboxNavigation.unregisterRoutesObserver(any())
            mapboxNavigation.unregisterRouteProgressObserver(any())
        }
        assertFalse(loader in trigger.observers)
    }

    private fun captureMapboxNavigationObserver(): MapboxNavigationCreateObserver {
        val mapboxNavigationObserverSlot = CapturingSlot<MapboxNavigationCreateObserver>()
        verify {
            LegacyMapboxNavigationInstanceHolder
                .registerCreateObserver(capture(mapboxNavigationObserverSlot))
        }
        return mapboxNavigationObserverSlot.captured
    }

    private fun captureTrigger(
        mapboxNavigationObserver: MapboxNavigationCreateObserver
    ): VoiceInstructionsDownloadTrigger {
        val routesObserverSlot = CapturingSlot<RoutesObserver>()
        mapboxNavigationObserver.onCreated(mapboxNavigation)
        verify { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) }
        return routesObserverSlot.captured as VoiceInstructionsDownloadTrigger
    }

    private fun checkRegistrantIsRegistered(loader: MapboxSpeechLoader) {
        val mapboxNavigationObserverSlot = CapturingSlot<MapboxNavigationCreateObserver>()
        val routesObserverSlot = CapturingSlot<RoutesObserver>()

        verify(exactly = 1) {
            LegacyMapboxNavigationInstanceHolder
                .registerCreateObserver(capture(mapboxNavigationObserverSlot))
        }

        mapboxNavigationObserverSlot.captured.onCreated(mapboxNavigation)

        verify(exactly = 1) { mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) }
        assertTrue(routesObserverSlot.captured is VoiceInstructionsDownloadTrigger)
        val trigger = routesObserverSlot.captured as VoiceInstructionsDownloadTrigger
        verify(exactly = 1) { mapboxNavigation.registerRouteProgressObserver(trigger) }
        assertTrue(loader in trigger.observers)
    }

    private fun checkRegistrantIsNotRegistered(loader: MapboxSpeechLoader) {
        verify(exactly = 0) { LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(any()) }
    }
}
