package com.mapbox.navigation.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

object ThreadController {
    private val maxCoresUsed = Runtime.getRuntime().availableProcessors().coerceAtMost(
        2
    )
    @UseExperimental(ObsoleteCoroutinesApi::class)
    private val IODispatchContext = newFixedThreadPoolContext(
        maxCoresUsed, "IODispatchContext"
    )
    private val rootJob = Job()
    val scope = CoroutineScope(rootJob + IODispatchContext)
}
