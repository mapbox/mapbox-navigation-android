package com.mapbox.navigation.ui.maps.util

import com.mapbox.navigation.testing.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
internal class MutexBasedScopeTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val originalScope = coroutineRule.createTestScope()
    private val mutexBasedScope = MutexBasedScope(originalScope)

    @Test
    fun scheduleMultipleTasks() = coroutineRule.runBlockingTest {
        lateinit var continuation1: Continuation<Unit>
        var called1 = false
        lateinit var continuation2: Continuation<Unit>
        var called2 = false
        var called3 = false
        mutexBasedScope.launchWithMutex {
            called1 = true
            suspendCancellableCoroutine { continuation1 = it }
        }
        assertTrue(called1)

        mutexBasedScope.launchWithMutex {
            called2 = true
            suspendCancellableCoroutine { continuation2 = it }
        }

        assertFalse(called2)

        continuation1.resume(Unit)

        assertTrue(called2)

        continuation2.resume(Unit)

        mutexBasedScope.launchWithMutex {
            called3 = true
        }
        assertTrue(called3)
    }

    @Test
    fun cancel() = coroutineRule.runBlockingTest {
        assertTrue(originalScope.isActive)
        val job = mutexBasedScope.launchWithMutex { delay(10000) }

        mutexBasedScope.cancelChildren()

        assertTrue(job.isCancelled)
        assertTrue(originalScope.isActive)

        val job2 = mutexBasedScope.launchWithMutex { delay(5) }
        // new jobs are accepted after cancel
        job2.join()
        assertTrue(job2.isCompleted)

//        originalScope.cancel()
    }
}
