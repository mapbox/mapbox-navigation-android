package com.mapbox.navigation.base.internal.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceTrackerTest {

    @Test
    fun `tracking section performance`() {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformanceSync("test-section") {
            "test-result"
        }

        assertEquals(
            listOf(
                ObserverInvocation.SyncSectionStarted("test-section"),
                ObserverInvocation.SyncSectionCompleted("test-section"),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("test-result", result)
    }

    @Test
    fun `exception during tracking`() {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result: Throwable = try {
            PerformanceTracker.trackPerformanceSync("test-section") {
                throw Throwable("test error")
            }
        } catch (t: Throwable) {
            t
        }

        assertEquals(
            listOf(
                ObserverInvocation.SyncSectionStarted("test-section"),
                ObserverInvocation.SyncSectionCompleted(
                    "test-section",
                    isDurationAvailable = false,
                ),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("test error", result.message)
    }

    @Test
    fun `nested tracking sections`() {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformanceSync("outer-section") {
            val inner1Result = PerformanceTracker.trackPerformanceSync("inner-section-1") {
                "inner1-value"
            }
            val inner2Result = PerformanceTracker.trackPerformanceSync("inner-section-2") {
                "inner2-value"
            }
            inner1Result + "+" + inner2Result + "-modified"
        }

        assertEquals(
            listOf(
                ObserverInvocation.SyncSectionStarted("outer-section"),
                ObserverInvocation.SyncSectionStarted("inner-section-1"),
                ObserverInvocation.SyncSectionCompleted("inner-section-1"),
                ObserverInvocation.SyncSectionStarted("inner-section-2"),
                ObserverInvocation.SyncSectionCompleted("inner-section-2"),
                ObserverInvocation.SyncSectionCompleted("outer-section"),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("inner1-value+inner2-value-modified", result)
    }

    @Test
    fun `exception during nested tracking`() {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val exception = try {
            PerformanceTracker.trackPerformanceSync("outer-section") {
                val inner1Result = PerformanceTracker.trackPerformanceSync("inner-section-1") {
                    "inner1-value"
                }
                val inner2Result = PerformanceTracker.trackPerformanceSync("inner-section-2") {
                    throw Throwable("inner section error")
                }
                inner1Result + "+" + inner2Result + "-modified"
            }
            null
        } catch (t: Throwable) {
            t
        }

        assertEquals(
            listOf(
                ObserverInvocation.SyncSectionStarted("outer-section"),
                ObserverInvocation.SyncSectionStarted("inner-section-1"),
                ObserverInvocation.SyncSectionCompleted("inner-section-1"),
                ObserverInvocation.SyncSectionStarted("inner-section-2"),
                ObserverInvocation.SyncSectionCompleted(
                    "inner-section-2",
                    isDurationAvailable = false,
                ),
                ObserverInvocation.SyncSectionCompleted(
                    "outer-section",
                    isDurationAvailable = false,
                ),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("inner section error", exception?.message)
    }
}

private sealed class ObserverInvocation {
    data class SyncSectionStarted(val sectionName: String) : ObserverInvocation()
    data class SyncSectionCompleted(
        val sectionName: String,
        val isDurationAvailable: Boolean = true,
    ) : ObserverInvocation()
}

/**
 * Fake observer implementation used for testing instead of mockk.
 * * Mockk's order verification (verifyOrder/verifySequence) was unreliable for this test case
 * due to issues with value classes (Duration) and nested call sequences. The fake observer
 * provides a more reliable and readable approach to verify the exact sequence of
 * performance tracking invocations.
 */
private class FakePerformanceObserver : PerformanceObserver {
    val recordedCalls = mutableListOf<ObserverInvocation>()

    override fun syncSectionStarted(name: String) {
        recordedCalls.add(ObserverInvocation.SyncSectionStarted(name))
    }

    override fun syncSectionCompleted(
        name: String,
        duration: kotlin.time.Duration?,
    ) {
        recordedCalls.add(
            ObserverInvocation.SyncSectionCompleted(
                name,
                isDurationAvailable = duration != null,
            ),
        )
    }
}
