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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.suspendCoroutine

@RunWith(RobolectricTestRunner::class)
class ThreadControllerTest {

    private val threadController = AndroidThreadController()

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
    fun checksCancelAllUICoroutines() {
        val mockedMainRootJob: CompletableJob = mockk(relaxed = true)
        threadController.mainRootJob = mockedMainRootJob

        threadController.cancelSDKScope()

        verify { mockedMainRootJob.cancelChildren() }
    }

    @Test
    fun checksGetMainScopeAndRootJob() {
        val mainRootJob = SupervisorJob()
        threadController.mainRootJob = mainRootJob

        val mainJobController = threadController.getSDKScopeAndRootJob()

        assertEquals(mainRootJob.children.first(), mainJobController.job)
        assertEquals(
            CoroutineScope(mainJobController.job + Dispatchers.Main).toString(),
            mainJobController.scope.toString()
        )
    }
}
