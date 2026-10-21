package com.mapbox.navigation.testing.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.concurrent.thread

class DelayedResponseModifierTest {

    @Test(timeout = LONG_DELAY_TEST_TIMEOUT_MS)
    fun `interrupt before the request is handled skips the delay`() {
        val modifier = DelayedResponseModifier(LONG_DELAY_MS)

        modifier.interruptDelay()

        assertEquals("response", modifier("response"))
    }

    @Test(timeout = LONG_DELAY_TEST_TIMEOUT_MS)
    fun `interrupt while the request is waiting ends the delay`() {
        val modifier = DelayedResponseModifier(LONG_DELAY_MS)
        var response: String? = null
        val handler = thread { response = modifier("response") }
        while (!handler.isBlocked()) {
            Thread.yield()
        }

        modifier.interruptDelay()
        handler.join()

        assertEquals("response", response)
    }

    @Test
    fun `interrupt before the request is handled affects only the next request`() {
        val modifier = DelayedResponseModifier(SHORT_DELAY_MS)
        modifier.interruptDelay()
        modifier("first")

        val startNanos = System.nanoTime()
        modifier("second")
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_IN_MILLI

        assertTrue("second request was not delayed: $elapsedMs ms", elapsedMs >= SHORT_DELAY_MS)
    }

    @Test(timeout = LONG_DELAY_TEST_TIMEOUT_MS)
    fun `original response modifier is applied after an interrupted delay`() {
        val modifier = DelayedResponseModifier(LONG_DELAY_MS) { "$it modified" }

        modifier.interruptDelay()

        assertEquals("response modified", modifier("response"))
    }

    private fun Thread.isBlocked() =
        state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING

    private companion object {
        const val LONG_DELAY_MS = 60_000L
        const val LONG_DELAY_TEST_TIMEOUT_MS = 10_000L
        const val SHORT_DELAY_MS = 300L
        const val NANOS_IN_MILLI = 1_000_000L
    }
}
