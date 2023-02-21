package com.mapbox.navigation.core.internal.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoroutineUtilsTest {

    private lateinit var parentJob: Job

    @Before
    fun setUp() {
        parentJob = SupervisorJob()
    }

    @After
    fun tearDown() {
        parentJob.cancel()
    }

    @Test
    fun createChildScope_parentCancellationCancelsChildren() {
        val scope1 = CoroutineUtils.createChildScope(parentJob)
        val scope2 = CoroutineUtils.createChildScope(parentJob)

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
    fun createChildScope_childDoesNotCancelOtherChildren() {
        val scope1 = CoroutineUtils.createChildScope(parentJob)
        val scope2 = CoroutineUtils.createChildScope(parentJob)

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
}
