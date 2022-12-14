package com.mapbox.navigation.testing

import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope

class TestThreadController(
    private val dispatcher: CoroutineDispatcher = TestCoroutineDispatcher(),
    private val scope: CoroutineScope = TestCoroutineScope(dispatcher + SupervisorJob())
) : ThreadController {

    override fun assertSDKThread() {
    }

    override fun getSDKScopeAndRootJob(immediate: Boolean): JobControl {
        val childScope = createChildSDKScope(immediate)
        return JobControl(childScope.coroutineContext.job, childScope)
    }

    override fun createChildSDKScope(immediate: Boolean): CoroutineScope {
        val job = SupervisorJob(scope.coroutineContext.job)
        return CoroutineScope(job + dispatcher)
    }

    override fun cancelSDKScope() {
        scope.cancel()
    }

    override fun getIODispatcher(): CoroutineDispatcher {
        return dispatcher
    }

    override fun getComputationDispatcher(): CoroutineDispatcher {
        return dispatcher
    }
}