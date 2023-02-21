package com.mapbox.navigation.core.internal.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal object CoroutineUtils {

    fun createChildScope(
        parentJob: Job,
        additionalContext: CoroutineContext = EmptyCoroutineContext
    ): CoroutineScope = CoroutineScope(SupervisorJob(parentJob) + additionalContext)
}
