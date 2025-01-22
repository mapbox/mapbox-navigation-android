package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineUtilsTest {

    @Test
    fun createChildScope_parentCancellationCancelsChildren() {
        val parentScope = createTestParentScope()
        val scope1 = parentScope.newChildScope()
        val scope2 = parentScope.newChildScope()

        val job1 = scope1.launch {
            delay(5000)
        }
        val job2 = scope2.launch {
            delay(5000)
        }

        assertFalse(job1.isCancelled)
        assertFalse(job2.isCancelled)

        parentScope.cancel()

        assertTrue(job1.isCancelled)
        assertTrue(job2.isCancelled)
    }

    @Test
    fun createChildScope_childDoesNotCancelOtherChildren() {
        val parentScope = createTestParentScope()
        val scope1 = parentScope.newChildScope()
        val scope2 = parentScope.newChildScope()

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
    fun createChildScope_childrenUseParentDispatcher() = runBlocking {
        val parentScope = createTestParentScope()
        val childScope = parentScope.newChildScope()

        var childThread: Long? = null
        var parentThread: Long? = null
        val childJob = childScope.launch {
            childThread = Thread.currentThread().id
        }
        val parentJob = parentScope.launch {
            parentThread = Thread.currentThread().id
        }

        parentJob.join()
        childJob.join()

        assertNotNull(childThread)
        assertEquals(parentThread, childThread)
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun createTestParentScope(): CoroutineScope {
    val parentDispatcher = newSingleThreadContext("patent thread")
    return CoroutineScope(
        SupervisorJob() + parentDispatcher,
    )
}
