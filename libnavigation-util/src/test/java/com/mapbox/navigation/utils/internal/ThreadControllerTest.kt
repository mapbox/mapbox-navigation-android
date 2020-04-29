package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
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
        val maxCoroutines = 10
        val maxDelay = 100L
        val jobControl = ThreadController.getIOScopeAndRootJob()
        (0 until maxCoroutines).forEach {
            jobControl.scope.launch {
                delay(maxDelay)
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
