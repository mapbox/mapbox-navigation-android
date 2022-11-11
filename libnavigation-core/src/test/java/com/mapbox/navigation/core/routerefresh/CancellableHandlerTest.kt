package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CancellableHandlerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val runnable = mockk<Runnable>(relaxed = true)
    private val cancellation = mockk<() -> Unit>(relaxed = true)
    private val handler = CancellableHandler(coroutineRule.coroutineScope)

    @Test
    fun postExecutesAfterTimeout() = coroutineRule.runBlockingTest {
        val timeout = 7000L

        handler.postDelayed(timeout, runnable, cancellation)

        verify(exactly = 0) {
            runnable.run()
            cancellation.invoke()
        }
        coroutineRule.testDispatcher.advanceTimeBy(timeout)
        verify(exactly = 1) {
            runnable.run()
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
        verify(exactly = 0) {
            runnable.run()
        }
    }

    @Test
    fun cancelAll_multipleJobs() = coroutineRule.runBlockingTest {
        val runnable2 = mockk<Runnable>(relaxed = true)
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

        verify(exactly = 1) { runnable.run() }
        verify(exactly = 0) { cancellation.invoke() }
    }
}
