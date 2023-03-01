package com.mapbox.navigation.core.internal.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext

internal object CoroutineUtils {

    fun createScope(
        parentJob: Job,
        additionalContext: CoroutineContext
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob(parentJob) + additionalContext)
    }

    fun createChildScope(
        parentScope: CoroutineScope
    ): CoroutineScope = CoroutineScope(
        SupervisorJob(parentScope.coroutineContext.job) + parentScope.coroutineContext.minusKey(Job)
    )
}
