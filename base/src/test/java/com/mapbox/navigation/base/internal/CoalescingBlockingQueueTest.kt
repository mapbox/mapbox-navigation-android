package com.mapbox.navigation.base.internal

import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CoalescingBlockingQueueTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val mutex = Mutex()

    private lateinit var queue: CoalescingBlockingQueue

    @Before
    fun setUp() {
        queue = CoalescingBlockingQueue(coroutineRule.coroutineScope, mutex)
    }

    @Test
    fun addAndExecuteSingleJob() = coroutineRule.runBlockingTest {
        val block = mockk<() -> Unit>(relaxed = true)
        val cancellation = mockk<() -> Unit>(relaxed = true)

        queue.addJob(CoalescingBlockingQueue.Item(block, cancellation))

        verify(exactly = 1) { block() }
        verify(exactly = 0) { cancellation() }
    }

    @Test
    fun addAndExecuteMultipleJobs() = coroutineRule.runBlockingTest {
        val block1 = mockk<() -> Unit>(relaxed = true)
        val cancellation1 = mockk<() -> Unit>(relaxed = true)
        val block2 = mockk<() -> Unit>(relaxed = true)
        val cancellation2 = mockk<() -> Unit>(relaxed = true)
        val block3 = mockk<() -> Unit>(relaxed = true)
        val cancellation3 = mockk<() -> Unit>(relaxed = true)
        val block4 = mockk<() -> Unit>(relaxed = true)
        val cancellation4 = mockk<() -> Unit>(relaxed = true)
        val block5 = mockk<() -> Unit>(relaxed = true)
        val cancellation5 = mockk<() -> Unit>(relaxed = true)
        val block6 = mockk<() -> Unit>(relaxed = true)
        val cancellation6 = mockk<() -> Unit>(relaxed = true)

        queue.addJob(CoalescingBlockingQueue.Item(block1, cancellation1))
        mutex.lock()
        queue.addJob(CoalescingBlockingQueue.Item(block2, cancellation2))
        queue.addJob(CoalescingBlockingQueue.Item(block3, cancellation3))
        queue.addJob(CoalescingBlockingQueue.Item(block4, cancellation4))
        mutex.unlock()
        queue.addJob(CoalescingBlockingQueue.Item(block5, cancellation5))
        queue.addJob(CoalescingBlockingQueue.Item(block6, cancellation6))

        verify(exactly = 1) { block1() }
        verify(exactly = 0) { cancellation1() }
        verify(exactly = 0) { block2() }
        verify(exactly = 1) { cancellation2() }
        verify(exactly = 0) { block3() }
        verify(exactly = 1) { cancellation3() }
        verify(exactly = 1) { block4() }
        verify(exactly = 0) { cancellation4() }
        verify(exactly = 1) { block5() }
        verify(exactly = 0) { cancellation5() }
        verify(exactly = 1) { block6() }
        verify(exactly = 0) { cancellation6() }
    }
}
