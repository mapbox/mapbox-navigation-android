package com.mapbox.navigation.core.internal

import com.mapbox.common.MemoryMonitorInterface
import com.mapbox.common.MemoryMonitorObserver
import com.mapbox.common.MemoryMonitorState
import com.mapbox.common.MemoryMonitorState.MEMORY_THRESHOLD_REACHED
import com.mapbox.common.MemoryMonitorState.SYSTEM_MEMORY_WARNING_RECEIVED
import com.mapbox.common.MemoryMonitorStatus
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LowMemoryManagerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val memoryMonitorObserverSlot = slot<MemoryMonitorObserver>()
    private lateinit var memoryMonitor: MemoryMonitorInterface
    private lateinit var lowMemoryManager: LowMemoryManager

    @Before
    fun setUp() {
        memoryMonitor = mockk(relaxed = true)
        every { memoryMonitor.registerObserver(capture(memoryMonitorObserverSlot)) } returns Unit

        lowMemoryManager = LowMemoryManagerImpl(memoryMonitor)
    }

    @Test
    fun subscribesToMemoryEventsWhenObserverAdded() {
        lowMemoryManager.addObserver(mockk(relaxed = true))
        verify(exactly = 1) {
            memoryMonitor.registerObserver(eq(memoryMonitorObserverSlot.captured))
        }
    }

    @Test
    fun subscribesToMemoryEventsOnceWhenMultiplesObserversAdded() {
        lowMemoryManager.addObserver(mockk(relaxed = true))
        lowMemoryManager.addObserver(mockk(relaxed = true))
        verify(exactly = 1) {
            memoryMonitor.registerObserver(eq(memoryMonitorObserverSlot.captured))
        }
    }

    @Test
    fun unsubscribesFromMemoryEventsWhenAllObserversRemoved() {
        val observer = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer)

        clearMocks(memoryMonitor)

        lowMemoryManager.removeObserver(observer)
        verify(exactly = 1) {
            memoryMonitor.unregisterObserver(eq(memoryMonitorObserverSlot.captured))
        }
    }

    @Test
    fun doesNotUnsubscribeFromMemoryEventsWhenThereAreObservers() {
        val observer = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer)
        lowMemoryManager.addObserver(mockk(relaxed = true))

        clearMocks(memoryMonitor)

        lowMemoryManager.removeObserver(observer)
        verify(exactly = 0) {
            memoryMonitor.unregisterObserver(any())
        }
    }

    @Test
    fun doesNotNotifyRemovedObserved() {
        val observer1 = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer1)

        val observer2 = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer2)

        lowMemoryManager.removeObserver(observer1)

        val status = createMemoryMonitorStatus(MEMORY_THRESHOLD_REACHED)
        memoryMonitorObserverSlot.captured.onMemoryMonitorAlert(status)

        verify(exactly = 0) {
            observer1.onLowMemory()
        }

        verify(exactly = 1) {
            observer2.onLowMemory()
        }
    }

    @Test
    fun notifiesObserversOnMemoryThresholdReachedEvent() {
        val observer = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer)

        val status = createMemoryMonitorStatus(MEMORY_THRESHOLD_REACHED)
        memoryMonitorObserverSlot.captured.onMemoryMonitorAlert(status)

        verify(exactly = 1) {
            observer.onLowMemory()
        }

        verify(exactly = 1) {
            logger.logD(
                category = "LowMemoryManager",
                msg = "onMemoryMonitorAlert($status). Notifying about low memory...",
            )
        }
    }

    @Test
    fun notifiesObserversOnSystemMemoryWarningEvent() {
        val observer = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer)

        val status = createMemoryMonitorStatus(SYSTEM_MEMORY_WARNING_RECEIVED)
        memoryMonitorObserverSlot.captured.onMemoryMonitorAlert(status)

        verify(exactly = 1) {
            observer.onLowMemory()
        }

        verify(exactly = 1) {
            logger.logD(
                category = "LowMemoryManager",
                msg = "onMemoryMonitorAlert($status). Notifying about low memory...",
            )
        }
    }

    @Test
    fun doesNotNotifyObserversOnOtherMemoryEvents() {
        val observer = mockk<LowMemoryManager.Observer>(relaxed = true)
        lowMemoryManager.addObserver(observer)

        MemoryMonitorState.values().forEach {
            when (it) {
                MEMORY_THRESHOLD_REACHED, SYSTEM_MEMORY_WARNING_RECEIVED -> {
                    // skip
                }
                else -> {
                    val status = createMemoryMonitorStatus(it)
                    memoryMonitorObserverSlot.captured.onMemoryMonitorAlert(status)
                }
            }
        }

        verify(exactly = 0) {
            observer.onLowMemory()
        }

        verify(exactly = 0) {
            logger.logD(any(), any())
        }
    }

    private fun createMemoryMonitorStatus(
        state: MemoryMonitorState,
        totalMemory: Long = 1000,
        usedMemory: Long = 700,
    ) = MemoryMonitorStatus(state, totalMemory, usedMemory)
}
