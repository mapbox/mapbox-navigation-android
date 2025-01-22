package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.plus

/**
 * Create a child scope which is connected to the parent([this]) lifetime and will be cancelled
 * as soon as parent scope is cancelled.
 * Solution is inspired by https://github.com/Kotlin/kotlinx.coroutines/issues/2758
 */
fun CoroutineScope.newChildScope(): CoroutineScope = this + Job(parent = coroutineContext[Job])
