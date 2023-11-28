package com.mapbox.navigation.utils.internal

import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.suspendCoroutine

class ThreadControllerTest {

    @get:Rule
    private val testCoroutineRule = MainCoroutineRule()

    private val threadController = ThreadController()

    @Test
    fun jobCountValidationNonUIScope() {
        val maxCoroutines = 100
        val jobControl = threadController.getIOScopeAndRootJob()
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
        threadController.ioRootJob = mockedIORootJob

        threadController.cancelAllNonUICoroutines()

        verify { mockedIORootJob.cancelChildren() }
    }

    @Test
    fun checksCancelAllUICoroutines() {
        val mockedMainRootJob: CompletableJob = mockk(relaxed = true)
        threadController.mainRootJob = mockedMainRootJob

        threadController.cancelAllUICoroutines()

        verify { mockedMainRootJob.cancelChildren() }
    }

    @Test
    fun checksGetIOScopeAndRootJob() {
        val ioRootJob = SupervisorJob()
        threadController.ioRootJob = ioRootJob

        val ioJobController = threadController.getIOScopeAndRootJob()

        assertEquals(ioRootJob.children.first(), ioJobController.job)
        assertEquals(
            CoroutineScope(ioJobController.job + ThreadController.IODispatcher).toString(),
            ioJobController.scope.toString()
        )
    }

    @Test
    fun checksGetMainScopeAndRootJob() {
        val mainRootJob = SupervisorJob()
        threadController.mainRootJob = mainRootJob

        val mainJobController = threadController.getMainScopeAndRootJob()

        assertEquals(mainRootJob.children.first(), mainJobController.job)
        assertEquals(
            CoroutineScope(mainJobController.job + Dispatchers.Main).toString(),
            mainJobController.scope.toString()
        )
    }

    @Test
    fun destroy_thread_controller() {
        val handler = CompletableDeferred<Unit>()
        threadController.getIOScopeAndRootJob().scope.launch {
            handler.await()
            fail("IO scope should be cancelled")
        }
        threadController.getMainScopeAndRootJob().scope.launch {
            handler.await()
            fail("UI scope should be cancelled")
        }
        threadController.destroy()
        handler.complete(Unit)
    }
}
