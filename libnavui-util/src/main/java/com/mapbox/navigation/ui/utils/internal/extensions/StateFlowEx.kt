package com.mapbox.navigation.ui.utils.internal.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

inline fun <T, R> StateFlow<T>.slice(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(),
    crossinline selector: (T) -> R,
): StateFlow<R> = map { selector(it) }.stateIn(scope, started, selector(value))
