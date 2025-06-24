package com.mapbox.navigation.core.internal.extensions

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.internal.history.HistoryRecorderChooser
import com.mapbox.navigation.core.internal.history.HistoryRecordingEnabledObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryRecorderChooserTest {

    private val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

    private val compositeRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private val manualRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private val copilotRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
    private val recorderObserver = mockk<(MapboxHistoryRecorder?) -> Unit>(relaxed = true)
    private lateinit var chooser: HistoryRecorderChooser

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Before
    fun setup() {
        every { mockMapboxNavigation.retrieveCompositeHistoryRecorder() } returns compositeRecorder
        every { mockMapboxNavigation.retrieveCopilotHistoryRecorder() } returns copilotRecorder
        every { mockMapboxNavigation.historyRecorder } returns manualRecorder
    }

    @Test
    fun `only copilot listener is registered and unregistered`() {
        chooser = HistoryRecorderChooser(false, true, mockMapboxNavigation, recorderObserver)
        verify(exactly = 0) {
            manualRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify {
            copilotRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.registerHistoryRecordingEnabledObserver(any())
        }

        clearAllMocks(answers = false)
        chooser.destroy()

        verify(exactly = 0) {
            manualRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify {
            copilotRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
    }

    @Test
    fun `only manual listener is registered`() {
        chooser = HistoryRecorderChooser(true, false, mockMapboxNavigation, recorderObserver)
        verify {
            manualRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            copilotRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.registerHistoryRecordingEnabledObserver(any())
        }

        clearAllMocks(answers = false)
        chooser.destroy()

        verify {
            manualRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            copilotRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
    }

    @Test
    fun `both listeners are registered`() {
        chooser = HistoryRecorderChooser(true, true, mockMapboxNavigation, recorderObserver)
        verify {
            manualRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify {
            copilotRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        // do not register a listener for composite recorder, because it is never used to do startRecording/stopRecording:
        // this is only a helper wrapper to push history events to both recorders simultaneously.
        verify(exactly = 0) {
            compositeRecorder.registerHistoryRecordingEnabledObserver(any())
        }

        clearAllMocks(answers = false)
        chooser.destroy()

        verify {
            manualRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify {
            copilotRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
    }

    @Test
    fun `no listeners are registered`() {
        chooser = HistoryRecorderChooser(false, false, mockMapboxNavigation, recorderObserver)
        verify(exactly = 0) {
            manualRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            copilotRecorder.registerHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.registerHistoryRecordingEnabledObserver(any())
        }

        clearAllMocks(answers = false)
        chooser.destroy()

        verify(exactly = 0) {
            manualRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            copilotRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
        verify(exactly = 0) {
            compositeRecorder.unregisterHistoryRecordingEnabledObserver(any())
        }
    }

    @Test
    fun `only copilot recording is enabled`() {
        val slot = slot<HistoryRecordingEnabledObserver>()
        chooser = HistoryRecorderChooser(false, true, mockMapboxNavigation, recorderObserver)
        verify {
            copilotRecorder.registerHistoryRecordingEnabledObserver(capture(slot))
        }

        slot.captured.onEnabled(mockk())

        verify { recorderObserver(copilotRecorder) }

        slot.captured.onDisabled(mockk())

        verify { recorderObserver(null) }
    }

    @Test
    fun `only manual recording is enabled`() {
        val slot = slot<HistoryRecordingEnabledObserver>()
        chooser = HistoryRecorderChooser(true, false, mockMapboxNavigation, recorderObserver)
        verify {
            manualRecorder.registerHistoryRecordingEnabledObserver(capture(slot))
        }

        slot.captured.onEnabled(mockk())

        verify { recorderObserver(manualRecorder) }

        slot.captured.onDisabled(mockk())

        verify { recorderObserver(null) }
    }

    @Test
    fun `both recordings are enabled`() {
        val copilotSlot = slot<HistoryRecordingEnabledObserver>()
        val manualSlot = slot<HistoryRecordingEnabledObserver>()
        chooser = HistoryRecorderChooser(true, true, mockMapboxNavigation, recorderObserver)
        verify {
            copilotRecorder.registerHistoryRecordingEnabledObserver(capture(copilotSlot))
        }
        verify {
            manualRecorder.registerHistoryRecordingEnabledObserver(capture(manualSlot))
        }

        copilotSlot.captured.onEnabled(mockk())

        verify { recorderObserver(copilotRecorder) }
        verify(exactly = 1) { recorderObserver(any()) }

        clearMocks(recorderObserver, answers = false)
        manualSlot.captured.onEnabled(mockk())

        verify { recorderObserver(compositeRecorder) }
        verify(exactly = 1) { recorderObserver(any()) }

        clearMocks(recorderObserver, answers = false)
        copilotSlot.captured.onDisabled(mockk())

        verify { recorderObserver(manualRecorder) }
        verify(exactly = 1) { recorderObserver(any()) }

        clearMocks(recorderObserver, answers = false)
        manualSlot.captured.onDisabled(mockk())

        verify { recorderObserver(null) }
        verify(exactly = 1) { recorderObserver(any()) }
    }
}
