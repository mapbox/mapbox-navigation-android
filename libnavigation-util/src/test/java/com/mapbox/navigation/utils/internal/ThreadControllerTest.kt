package com.mapbox.navigation.utils.internal

import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadControllerTest {

    @Test
    fun jobCountValidationNonUIScope() {
        val maxCoroutines = 100
        val jobControl = ThreadController.getIOScopeAndRootJob()
        repeat(maxCoroutines) {
            jobControl.scope.launch {
                suspendCoroutine {
                    // do nothing. Just not to finish a coroutine
                }
            }
        }
        assertTrue(jobControl.job.children.count() == maxCoroutines)
        jobControl.job.cancel()
        assertTrue(jobControl.job.isCancelled)
    }

    @Test
    fun monitorChannelWithException_callsOnCancellation_whenChannelClosed() {
        var flag = false
        val channel: Channel<String> = Channel()

        runBlocking {
            monitorChannelWithException(
                channel,
                {},
                { flag = true }
            )

            channel.send("foobar")

            assertFalse(flag)

            channel.close(ClosedReceiveChannelException(""))
            try {
                channel.send("foobar")
            } catch (ex: Exception) {
            }
        }
        assertTrue(flag)
    }

    @Test
    fun monitorChannelWithException() {
        val channel: Channel<String> = Channel()
        var msg = "error"

        runBlocking {
            monitorChannelWithException(
                channel,
                {
                    msg = it
                },
                {}
            )

            channel.send("success")
            channel.close()
        }

        assertThat(msg, `is`("success"))
    }
}
