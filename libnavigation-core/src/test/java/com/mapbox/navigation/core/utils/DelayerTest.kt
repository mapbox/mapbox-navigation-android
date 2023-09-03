package com.mapbox.navigation.core.utils

import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DelayerTest {

    private val interval = 5000L
    private val delayer = Delayer(interval)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkObject(Time.SystemClockImpl)
        every { Time.SystemClockImpl.millis() } answers {
            coroutineRule.testDispatcher.currentTime
        }
    }

    @After
    fun tearDown() {
        unmockkObject(Time.SystemClockImpl)
    }

    @Test
    fun resumeDelayWithoutDelay() = coroutineRule.runBlockingTest {
        val job = launch {
            delayer.resumeDelay()
        }

        coroutineRule.testDispatcher.advanceTimeBy(4999)

        assertFalse(job.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job.isCompleted)
    }

    @Test
    fun resumeDelayWithDelayThatJustStarted() = coroutineRule.runBlockingTest {
        val job1 = launch { delayer.delay() }
        job1.cancel()

        val job = launch {
            delayer.resumeDelay()
        }

        coroutineRule.testDispatcher.advanceTimeBy(4999)

        assertFalse(job.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job.isCompleted)
    }

    @Test
    fun resumeDelayWithDelayThatCompletedPartially() = coroutineRule.runBlockingTest {
        val job1 = launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)
        job1.cancel()

        val job = launch {
            delayer.resumeDelay()
        }

        coroutineRule.testDispatcher.advanceTimeBy(1999)

        assertFalse(job.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job.isCompleted)
    }

    @Test
    fun resumeDelayWithDelayThatCompleted() = coroutineRule.runBlockingTest {
        launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(5000)

        val job = launch {
            delayer.resumeDelay()
        }

        assertTrue(job.isCompleted)
    }

    @Test
    fun resumeDelayWithSecondDelayThatCompletedPartially() = coroutineRule.runBlockingTest {
        launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(5000)

        val job1 = launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)
        job1.cancel()

        val job = launch {
            delayer.resumeDelay()
        }

        coroutineRule.testDispatcher.advanceTimeBy(1999)

        assertFalse(job.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job.isCompleted)
    }

    @Test
    fun resumeDelayAfterResumeDelayThatJustStarted() = coroutineRule.runBlockingTest {
        val job1 = launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)
        job1.cancel()

        val job2 = launch { delayer.resumeDelay() }
        job2.cancel()

        val job3 = launch { delayer.resumeDelay() }
        coroutineRule.testDispatcher.advanceTimeBy(1999)

        assertFalse(job3.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job3.isCompleted)
    }

    @Test
    fun resumeDelayAfterResumeDelayThatCompletedPartially() = coroutineRule.runBlockingTest {
        val job1 = launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)
        job1.cancel()

        val job2 = launch { delayer.resumeDelay() }
        coroutineRule.testDispatcher.advanceTimeBy(1500)
        job2.cancel()

        val job3 = launch { delayer.resumeDelay() }
        coroutineRule.testDispatcher.advanceTimeBy(499)

        assertFalse(job3.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job3.isCompleted)
    }

    @Test
    fun resumeDelayAfterResumeDelayThatCompleted() = coroutineRule.runBlockingTest {
        val job1 = launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)
        job1.cancel()

        launch { delayer.resumeDelay() }
        coroutineRule.testDispatcher.advanceTimeBy(2000)

        val job2 = launch { delayer.resumeDelay() }

        assertTrue(job2.isCompleted)
    }

    @Test
    fun resumeDelayWhenDelayIsInProgress() = coroutineRule.runBlockingTest {
        launch { delayer.delay() }
        coroutineRule.testDispatcher.advanceTimeBy(3000)

        val job2 = launch { delayer.resumeDelay() }

        coroutineRule.testDispatcher.advanceTimeBy(4999)

        assertFalse(job2.isCompleted)

        coroutineRule.testDispatcher.advanceTimeBy(1)

        assertTrue(job2.isCompleted)
    }
}
