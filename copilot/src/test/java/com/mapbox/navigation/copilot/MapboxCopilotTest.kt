package com.mapbox.navigation.copilot

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.CopilotTestUtils.prepareLifecycleOwnerMockk
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxCopilotTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    @After
    fun teardown() {
        MapboxCopilot.onDetached(mockedMapboxNavigation)
        unmockkAll()
    }

    @Test
    fun `onAttached starts MapboxCopilotImpl`() {
        prepareLifecycleOwnerMockk()
        prepareMapboxCopilotImplStart()

        MapboxCopilot.onAttached(mockedMapboxNavigation)

        verify(exactly = 1) {
            anyConstructed<MapboxCopilotImpl>().start()
        }
    }

    @Test
    fun `start is called only once if onAttached is called multiple times`() {
        prepareLifecycleOwnerMockk()
        prepareMapboxCopilotImplStart()

        MapboxCopilot.onAttached(mockedMapboxNavigation)
        MapboxCopilot.onAttached(mockedMapboxNavigation)
        MapboxCopilot.onAttached(mockedMapboxNavigation)

        verify(exactly = 1) {
            anyConstructed<MapboxCopilotImpl>().start()
        }
    }

    @Test
    fun `onDetached stops MapboxCopilotImpl`() {
        prepareLifecycleOwnerMockk()
        prepareMapboxCopilotImplStart()
        every { anyConstructed<MapboxCopilotImpl>().stop() } just Runs
        MapboxCopilot.onAttached(mockedMapboxNavigation)

        MapboxCopilot.onDetached(mockedMapboxNavigation)

        verify(exactly = 1) {
            anyConstructed<MapboxCopilotImpl>().stop()
        }
    }

    @Test
    fun `MapboxCopilotImpl stop is called only once if onDetached is called multiple times`() {
        prepareLifecycleOwnerMockk()
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        prepareMapboxCopilotImplStart()
        every { anyConstructed<MapboxCopilotImpl>().stop() } just Runs
        MapboxCopilot.onAttached(mockedMapboxNavigation)

        MapboxCopilot.onDetached(mockedMapboxNavigation)
        MapboxCopilot.onDetached(mockedMapboxNavigation)
        MapboxCopilot.onDetached(mockedMapboxNavigation)

        verify(exactly = 1) {
            anyConstructed<MapboxCopilotImpl>().stop()
        }
    }

    @Test
    fun `events are pushed if MapboxCopilotImpl is started`() {
        prepareLifecycleOwnerMockk()
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        prepareMapboxCopilotImplStart()
        val mockedHistoryEvent = mockk<HistoryEvent>(relaxed = true)
        every { anyConstructed<MapboxCopilotImpl>().push(any()) } just Runs
        MapboxCopilot.onAttached(mockedMapboxNavigation)

        MapboxCopilot.push(mockedHistoryEvent)

        verify(exactly = 1) {
            anyConstructed<MapboxCopilotImpl>().push(eq(mockedHistoryEvent))
        }
    }

    @Test
    fun `events are not pushed if MapboxCopilotImpl is not started`() {
        prepareLifecycleOwnerMockk()
        prepareMapboxCopilotImplStart()
        val mockedHistoryEvent = mockk<HistoryEvent>(relaxed = true)
        every { anyConstructed<MapboxCopilotImpl>().push(any()) } just Runs

        MapboxCopilot.push(mockedHistoryEvent)

        verify(exactly = 0) {
            anyConstructed<MapboxCopilotImpl>().push(eq(mockedHistoryEvent))
        }
    }

    @Test
    fun `events are not pushed after MapboxCopilotImpl is stopped`() {
        prepareLifecycleOwnerMockk()
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        prepareMapboxCopilotImplStart()
        val mockedHistoryEvent = mockk<HistoryEvent>(relaxed = true)
        every { anyConstructed<MapboxCopilotImpl>().stop() } just Runs
        every { anyConstructed<MapboxCopilotImpl>().push(any()) } just Runs
        MapboxCopilot.onAttached(mockedMapboxNavigation)

        MapboxCopilot.onDetached(mockedMapboxNavigation)
        MapboxCopilot.push(mockedHistoryEvent)

        verify(exactly = 0) {
            anyConstructed<MapboxCopilotImpl>().push(eq(mockedHistoryEvent))
        }
    }

    private fun prepareMapboxCopilotImplStart() {
        mockkConstructor(MapboxCopilotImpl::class)
        every { anyConstructed<MapboxCopilotImpl>().start() } just Runs
    }
}
