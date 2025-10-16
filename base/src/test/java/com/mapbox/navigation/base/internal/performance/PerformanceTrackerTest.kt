package com.mapbox.navigation.base.internal.performance

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.collections.emptyList

class PerformanceTrackerTest {

    @org.junit.Before
    fun setUp() {
        PerformanceTracker.resetStateForTests()
    }

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

    @Test
    fun `tracking async section performance`() = runBlocking {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformanceAsync("test-async-section") {
            "test-async-result"
        }

        assertEquals(
            listOf(
                ObserverInvocation.AsyncSectionStarted("test-async-section", 1),
                ObserverInvocation.AsyncSectionCompleted("test-async-section", 1),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("test-async-result", result)
    }

    // as performance optimization, tracker doesn't create objects in case there are no observers
    // to that sections started before the first observer was registered aren't logged
    @Test
    fun `first observer is added during async section`() = runBlocking {
        PerformanceTracker.resetStateForTests()
        val testSection = PerformanceTracker.asyncSectionStarted("test1")
        val testObserver = FakePerformanceObserver()

        PerformanceTracker.addObserver(testObserver)
        PerformanceTracker.asyncSectionCompleted(testSection)

        assertEquals(
            emptyList<ObserverInvocation>(),
            testObserver.recordedCalls,
        )
    }

    @Test
    fun `exception during async tracking`() = runBlocking {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result: Throwable = try {
            PerformanceTracker.trackPerformanceAsync("test-async-section") {
                throw Throwable("test async error")
            }
        } catch (t: Throwable) {
            t
        }

        assertEquals(
            listOf(
                ObserverInvocation.AsyncSectionStarted("test-async-section", 1),
                ObserverInvocation.AsyncSectionCompleted(
                    "test-async-section",
                    1,
                ),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("test async error", result.message)
    }

    @Test
    fun `nested async tracking sections`() = runBlocking {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformanceAsync("outer-async-section") {
            val inner1Result = PerformanceTracker.trackPerformanceAsync("inner-async-section-1") {
                "inner1-async-value"
            }
            val inner2Result = PerformanceTracker.trackPerformanceAsync("inner-async-section-2") {
                "inner2-async-value"
            }
            inner1Result + "+" + inner2Result + "-async-modified"
        }

        assertEquals(
            listOf(
                ObserverInvocation.AsyncSectionStarted("outer-async-section", 1),
                ObserverInvocation.AsyncSectionStarted("inner-async-section-1", 2),
                ObserverInvocation.AsyncSectionCompleted("inner-async-section-1", 2),
                ObserverInvocation.AsyncSectionStarted("inner-async-section-2", 3),
                ObserverInvocation.AsyncSectionCompleted("inner-async-section-2", 3),
                ObserverInvocation.AsyncSectionCompleted("outer-async-section", 1),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("inner1-async-value+inner2-async-value-async-modified", result)
    }

    @Test
    fun `mixed sync and async tracking sections`() = runBlocking {
        val testObserver = FakePerformanceObserver()
        PerformanceTracker.addObserver(testObserver)

        val result = PerformanceTracker.trackPerformanceSync("sync-section") {
            val asyncResult = PerformanceTracker.trackPerformanceAsync("async-section") {
                "async-value"
            }
            val syncResult = PerformanceTracker.trackPerformanceSync("inner-sync-section") {
                "sync-value"
            }
            asyncResult + "+" + syncResult + "-mixed"
        }

        assertEquals(
            listOf(
                ObserverInvocation.SyncSectionStarted("sync-section"),
                ObserverInvocation.AsyncSectionStarted("async-section", 1),
                ObserverInvocation.AsyncSectionCompleted("async-section", 1),
                ObserverInvocation.SyncSectionStarted("inner-sync-section"),
                ObserverInvocation.SyncSectionCompleted("inner-sync-section"),
                ObserverInvocation.SyncSectionCompleted("sync-section"),
            ),
            testObserver.recordedCalls,
        )
        assertEquals("async-value+sync-value-mixed", result)
    }
}

private sealed class ObserverInvocation {
    data class SyncSectionStarted(val sectionName: String) : ObserverInvocation()
    data class SyncSectionCompleted(
        val sectionName: String,
        val isDurationAvailable: Boolean = true,
    ) : ObserverInvocation()
    data class AsyncSectionStarted(val sectionName: String, val id: Int) : ObserverInvocation()
    data class AsyncSectionCompleted(
        val sectionName: String,
        val id: Int,
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

    override fun asyncSectionStarted(name: String, id: Int) {
        recordedCalls.add(ObserverInvocation.AsyncSectionStarted(name, id))
    }

    override fun asyncSectionFinished(
        name: String,
        id: Int,
        duration: kotlin.time.Duration?,
    ) {
        recordedCalls.add(
            ObserverInvocation.AsyncSectionCompleted(
                name,
                id,
                isDurationAvailable = duration != null,
            ),
        )
    }
}
