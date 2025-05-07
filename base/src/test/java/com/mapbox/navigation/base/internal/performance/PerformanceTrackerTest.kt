package com.mapbox.navigation.base.internal.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration

class PerformanceTrackerTest {

    @Test
    fun `tracking section performance`() {
        val testObserver = TestPerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformance("test-section") {
            assertEquals("test-section", testObserver.latestSectionStartedName)
            assertNull(testObserver.latestSectionCompletedName)
            assertNull(testObserver.latestDuration)
            "test-result"
        }

        assertEquals("test-section", testObserver.latestSectionCompletedName)
        assertNotNull(testObserver.latestDuration)
        assertEquals("test-result", result)
    }

    @Test
    fun `exception during tracking`() {
        val testObserver = TestPerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result: Throwable = try {
            PerformanceTracker.trackPerformance("test-section") {
                throw Throwable("test error")
            }
        } catch (t: Throwable) {
            t
        }

        assertEquals("test-section", testObserver.latestSectionStartedName)
        assertEquals("test-section", testObserver.latestSectionCompletedName)
        assertNull(testObserver.latestDuration)
        assertEquals("test error", result.message)
    }
}

class TestPerformanceObserver : PerformanceObserver {

    var latestSectionStartedName: String? = null
        private set
    var latestSectionCompletedName: String? = null
        private set
    var latestDuration: Duration? = null
        private set

    override fun sectionStarted(name: String) {
        latestSectionStartedName = name
    }

    override fun sectionCompleted(name: String, duration: Duration?) {
        latestSectionCompletedName = name
        latestDuration = duration
    }
}
