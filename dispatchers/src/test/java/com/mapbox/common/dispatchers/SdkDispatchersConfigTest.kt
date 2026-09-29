@file:Suppress("ForbiddenImport")

package com.mapbox.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class SdkDispatchersConfigTest {

    private val customExecutor = Executors.newFixedThreadPool(8)
    private val customDispatcher = customExecutor.asCoroutineDispatcher()

    @After
    fun tearDown() {
        customExecutor.shutdownNow()
    }

    @Test
    fun noCustomizations_defaultDispatcher_isUnmodified() {
        val config = SdkDispatchersConfig.Builder().build()

        assertSame(Dispatchers.Default, config.defaultDispatcher)
    }

    @Test
    fun noCustomizations_ioDispatcher_isUnmodified() {
        val config = SdkDispatchersConfig.Builder().build()

        assertSame(Dispatchers.IO, config.ioDispatcher)
    }

    @Test
    fun setDefaultParallelism_limitsBuiltInDefaultDispatcher() {
        val config = SdkDispatchersConfig.Builder()
            .setDefaultParallelism(2)
            .build()

        assertEquals(2, peakConcurrency(config.defaultDispatcher, taskCount = 7))
    }

    @Test
    fun setIoParallelism_limitsBuiltInIoDispatcher() {
        val config = SdkDispatchersConfig.Builder()
            .setIoParallelism(6)
            .build()

        assertEquals(6, peakConcurrency(config.ioDispatcher, taskCount = 11))
    }

    @Test
    fun setDefault_withoutParallelism_usesCustomDispatcherUnmodified() {
        val config = SdkDispatchersConfig.Builder()
            .setDefault(customDispatcher)
            .build()

        assertSame(customDispatcher, config.defaultDispatcher)
    }

    @Test
    fun setIO_withoutParallelism_usesCustomDispatcherUnmodified() {
        val config = SdkDispatchersConfig.Builder()
            .setIO(customDispatcher)
            .build()

        assertSame(customDispatcher, config.ioDispatcher)
    }

    @Test
    fun setDefault_withParallelism_limitsCustomDispatcherOnTop() {
        val config = SdkDispatchersConfig.Builder()
            .setDefault(customDispatcher)
            .setDefaultParallelism(3)
            .build()

        assertNotSame(customDispatcher, config.defaultDispatcher)
        assertEquals(3, peakConcurrency(config.defaultDispatcher, taskCount = 8))
    }

    @Test
    fun setIO_withParallelism_limitsCustomDispatcherOnTop() {
        val config = SdkDispatchersConfig.Builder()
            .setIO(customDispatcher)
            .setIoParallelism(4)
            .build()

        assertNotSame(customDispatcher, config.ioDispatcher)
        assertEquals(4, peakConcurrency(config.ioDispatcher, taskCount = 8))
    }

    @Test
    fun default_matchesUnconfiguredBuilder() {
        val fromFactory = SdkDispatchersConfig.default()

        assertSame(Dispatchers.Default, fromFactory.defaultDispatcher)
        assertSame(Dispatchers.IO, fromFactory.ioDispatcher)
    }

    /**
     * Runs [taskCount] blocking tasks on [dispatcher] and returns the highest number of them
     * observed running at the same time. Uses a blocking [Thread.sleep] rather than a
     * suspending delay, since `limitedParallelism` only limits concurrently *executing* work -
     * a suspended coroutine does not hold a dispatcher slot and would not reveal a
     * missing/incorrect limit.
     */
    private fun peakConcurrency(
        dispatcher: CoroutineDispatcher,
        taskCount: Int,
    ): Int = runBlocking {
        val running = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val jobs = (1..taskCount).map {
            launch(dispatcher) {
                val current = running.incrementAndGet()
                peak.updateAndGet { max -> maxOf(max, current) }
                Thread.sleep(100)
                running.decrementAndGet()
            }
        }
        jobs.joinAll()
        peak.get()
    }
}
