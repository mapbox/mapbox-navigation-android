package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CancellableHandlerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val runnable = mockk<suspend () -> Unit>(relaxed = true)
    private val cancellation = mockk<() -> Unit>(relaxed = true)
    private val testScope = coroutineRule.createTestScope()
    private val handler = CancellableHandler(testScope)

    @Test
    fun postExecutesWithZeroTimeout() = coroutineRule.runBlockingTest {
        handler.postDelayed(0, runnable, cancellation)

        coVerify(exactly = 1) {
            runnable()
        }
        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun postExecutesWithNegativeTimeout() = coroutineRule.runBlockingTest {
        handler.postDelayed(-10, runnable, cancellation)

        coVerify(exactly = 1) {
            runnable()
        }
        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun postExecutesAfterTimeout() = coroutineRule.runBlockingTest {
        val timeout = 7000L

        handler.postDelayed(timeout, runnable, cancellation)

        coVerify(exactly = 0) {
            runnable()
            cancellation.invoke()
        }
        coroutineRule.testDispatcher.advanceTimeBy(timeout)
        coVerify(exactly = 1) {
            runnable()
        }
        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun postRemovesBlockFromMap() = coroutineRule.runBlockingTest {
        val timeout = 7000L
        handler.postDelayed(timeout, runnable, cancellation)
        coroutineRule.testDispatcher.advanceTimeBy(timeout)

        handler.cancelAll()

        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun postWithZeroTimeoutRemovesBlockFromMap() = coroutineRule.runBlockingTest {
        handler.postDelayed(0, runnable, cancellation)

        handler.cancelAll()

        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun postWithNegativeTimeoutRemovesBlockFromMap() = coroutineRule.runBlockingTest {
        handler.postDelayed(-10, runnable, cancellation)

        handler.cancelAll()

        verify(exactly = 0) {
            cancellation.invoke()
        }
    }

    @Test
    fun cancelScope_noJobs() = coroutineRule.runBlockingTest {
        testScope.cancel()
    }

    @Test
    fun cancelScope_hasRunningJob() = coroutineRule.runBlockingTest {
        coEvery { runnable() } coAnswers { delay(1000) }
        handler.postDelayed(0, runnable, cancellation)

        testScope.cancel()

        coVerify(exactly = 1) { cancellation() }
    }

    @Test
    fun cancelScope_hasCompletedJob() = coroutineRule.runBlockingTest {
        coEvery { runnable() } coAnswers { delay(1000) }
        handler.postDelayed(0, runnable, cancellation)
        coroutineRule.testDispatcher.advanceTimeBy(1000)

        testScope.cancel()

        coVerify(exactly = 0) { cancellation() }
    }

    @Test
    fun cancelAll_noJobs() = coroutineRule.runBlockingTest {
        handler.cancelAll()
    }

    @Test
    fun cancelAll_hasIncompleteJob() = coroutineRule.runBlockingTest {
        handler.postDelayed(1000, runnable, cancellation)

        handler.cancelAll()

        verify(exactly = 1) {
            cancellation.invoke()
        }
        coroutineRule.testDispatcher.advanceTimeBy(1000)
        coVerify(exactly = 0) {
            runnable()
        }
    }

    @Test
    fun cancelAll_multipleJobs() = coroutineRule.runBlockingTest {
        val runnable2 = mockk<suspend () -> Unit>(relaxed = true)
        val cancellation2 = mockk<() -> Unit>(relaxed = true)
        handler.postDelayed(1000, runnable, cancellation)
        handler.postDelayed(500, runnable2, cancellation2)

        handler.cancelAll()

        verify(exactly = 1) {
            cancellation.invoke()
            cancellation2.invoke()
        }
    }

    @Test
    fun postJobAfterItHasBeenCancel() = coroutineRule.runBlockingTest {
        handler.postDelayed(1000, runnable, cancellation)
        handler.cancelAll()
        clearAllMocks(answers = false)

        handler.postDelayed(1000, runnable, cancellation)
        coroutineRule.testDispatcher.advanceTimeBy(1000)

        coVerify(exactly = 1) { runnable() }
        verify(exactly = 0) { cancellation.invoke() }
    }
}
