package com.mapbox.navigation.utils.internal

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.suspendCoroutine

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

    @Test
    fun checksCancelAllNonUICoroutines() {
        val mockedIORootJob: CompletableJob = mockk(relaxed = true)
        ThreadController.ioRootJob = mockedIORootJob

        ThreadController.cancelAllNonUICoroutines()

        verify { mockedIORootJob.cancelChildren() }
    }

    @Test
    fun checksCancelAllUICoroutines() {
        val mockedMainRootJob: CompletableJob = mockk(relaxed = true)
        ThreadController.mainRootJob = mockedMainRootJob

        ThreadController.cancelAllUICoroutines()

        verify { mockedMainRootJob.cancelChildren() }
    }

    @Test
    fun checksGetIOScopeAndRootJob() {
        val ioRootJob = SupervisorJob()
        ThreadController.ioRootJob = ioRootJob

        val ioJobController = ThreadController.getIOScopeAndRootJob()

        assertEquals(ioRootJob.children.first(), ioJobController.job)
        assertEquals(
            CoroutineScope(ioJobController.job + ThreadController.IODispatcher).toString(),
            ioJobController.scope.toString()
        )
    }

    @Test
    fun checksGetMainScopeAndRootJob() {
        val mainRootJob = SupervisorJob()
        ThreadController.mainRootJob = mainRootJob

        val mainJobController = ThreadController.getMainScopeAndRootJob()

        assertEquals(mainRootJob.children.first(), mainJobController.job)
        assertEquals(
            CoroutineScope(mainJobController.job + Dispatchers.Main).toString(),
            mainJobController.scope.toString()
        )
    }
}
