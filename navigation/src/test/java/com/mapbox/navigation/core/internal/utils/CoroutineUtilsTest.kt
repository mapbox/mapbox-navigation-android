package com.mapbox.navigation.core.internal.utils

import android.os.Handler
import android.os.HandlerThread
import com.mapbox.navigation.core.internal.utils.CoroutineUtils.withTimeoutOrDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
class CoroutineUtilsTest {

    private lateinit var parentThread: HandlerThread
    private lateinit var parentScope: CoroutineScope
    private lateinit var parentJob: Job

    @Before
    fun setUp() {
        parentThread = HandlerThread("parent thread").also { it.start() }
        parentJob = SupervisorJob()
        parentScope = CoroutineScope(
            SupervisorJob() + Handler(parentThread.looper).asCoroutineDispatcher(),
        )
    }

    @After
    fun tearDown() {
        parentScope.cancel()
        parentJob.cancel()
        parentThread.quit()
    }

    @Test
    fun createScope_parentCancellationCancelsChildren() {
        val scope1 = CoroutineUtils.createScope(parentJob, EmptyCoroutineContext)
        val scope2 = CoroutineUtils.createScope(parentJob, EmptyCoroutineContext)

        val job1 = scope1.launch {
            delay(5000)
        }
        val job2 = scope2.launch {
            delay(5000)
        }

        assertFalse(job1.isCancelled)
        assertFalse(job2.isCancelled)

        parentJob.cancel()

        assertTrue(job1.isCancelled)
        assertTrue(job2.isCancelled)
    }

    @Test
    fun createScope_childDoesNotCancelOtherChildren() {
        val scope1 = CoroutineUtils.createScope(parentJob, EmptyCoroutineContext)
        val scope2 = CoroutineUtils.createScope(parentJob, EmptyCoroutineContext)

        val job1 = scope1.launch {
            delay(5000)
        }
        val job2 = scope2.launch {
            delay(5000)
        }

        assertFalse(job1.isCancelled)
        assertFalse(job2.isCancelled)

        scope1.cancel()

        assertTrue(job1.isCancelled)
        assertFalse(job2.isCancelled)
    }

    @Test
    fun createScope_childrenUseOwnDispatcher() = runBlocking {
        val thread1 = HandlerThread("thread 1").also { it.start() }
        val thread2 = HandlerThread("thread 2").also { it.start() }
        try {
            val childScope1 = CoroutineUtils.createScope(
                parentJob,
                Handler(thread1.looper).asCoroutineDispatcher(),
            )
            val childScope2 = CoroutineUtils.createScope(
                parentJob,
                Handler(thread2.looper).asCoroutineDispatcher(),
            )

            var childThread1: Thread? = null
            var childThread2: Thread? = null
            val childJob1 = childScope1.launch {
                childThread1 = Thread.currentThread()
            }
            val childJob2 = childScope2.launch {
                childThread2 = Thread.currentThread()
            }

            childJob1.join()
            childJob2.join()

            assertEquals(thread1, childThread1)
            assertEquals(thread2, childThread2)
        } finally {
            thread1.quit()
            thread2.quit()
        }
    }

    @Test
    fun withTimeoutOrDefault_success() = runBlocking {
        val actual = withTimeoutOrDefault(timeMillis = 1000, default = 0) {
            10
        }

        assertEquals(10, actual)
    }

    @Test
    fun withTimeoutOrDefault_timeout() = runBlocking {
        val actual = withTimeoutOrDefault(timeMillis = 5, default = 1) {
            delay(6)
            10
        }

        assertEquals(1, actual)
    }
}
