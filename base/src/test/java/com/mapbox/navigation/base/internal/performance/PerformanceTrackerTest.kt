package com.mapbox.navigation.base.internal.performance

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceTrackerTest {

    @Test
    fun `tracking section performance`() {
        val testObserver = mockk<PerformanceObserver>(relaxed = true)
        PerformanceTracker.addObserver(testObserver)
        val sectionId = slot<Int>()

        val result = PerformanceTracker.trackPerformance("test-section") {
            verify { testObserver.sectionStarted("test-section", capture(sectionId)) }
            "test-result"
        }

        verify { testObserver.sectionCompleted("test-section", sectionId.captured, any()) }
        assertEquals("test-result", result)
    }

    @Test
    fun `exception during tracking`() {
        val testObserver = mockk<PerformanceObserver>(relaxed = true)
        PerformanceTracker.addObserver(testObserver)

        val result: Throwable = try {
            PerformanceTracker.trackPerformance("test-section") {
                throw Throwable("test error")
            }
        } catch (t: Throwable) {
            t
        }

        val sectionId = slot<Int>()
        verify { testObserver.sectionStarted("test-section", capture(sectionId)) }
        verify { testObserver.sectionCompleted("test-section", capture(sectionId), null) }
        assertEquals("test error", result.message)
    }
}
