package com.mapbox.navigation.testing

import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope

class TestThreadController(
    private val scope: CoroutineScope = TestCoroutineScope(TestCoroutineDispatcher() + Job())
) : ThreadController {

    override fun assertSDKThread() {
    }

    override fun getSDKScopeAndRootJob(immediate: Boolean): JobControl {
        return JobControl(scope.coroutineContext.job, scope)
    }

    override fun cancel() {
        scope.cancel()
    }
}