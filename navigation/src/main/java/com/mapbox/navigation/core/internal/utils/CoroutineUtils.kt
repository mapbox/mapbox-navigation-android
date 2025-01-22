package com.mapbox.navigation.core.internal.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext

internal object CoroutineUtils {

    fun createScope(
        parentJob: Job,
        additionalContext: CoroutineContext,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob(parentJob) + additionalContext)
    }

    suspend fun <T : Any> withTimeoutOrDefault(
        timeMillis: Long,
        default: T,
        block: suspend CoroutineScope.() -> T,
    ): T {
        return withTimeoutOrNull(timeMillis, block) ?: default
    }
}
