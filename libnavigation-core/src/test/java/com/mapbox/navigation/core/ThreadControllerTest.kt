package com.mapbox.navigation.core

import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Test

const val MAX_COROUTINES = 10
const val MAX_DELAY = 100L

class ThreadControllerTest {
    @Test
    fun jobCountValidationNonUIScope() {
        val jobControl = ThreadController.getIOScopeAndRootJob()
        (0 until MAX_COROUTINES).forEach {
            jobControl.scope.launch {
                delay(MAX_DELAY)
            }
        }
        assertTrue(jobControl.job.children.count() == MAX_COROUTINES)
        jobControl.job.cancel()
        assertTrue(jobControl.job.isCancelled)
    }
}
