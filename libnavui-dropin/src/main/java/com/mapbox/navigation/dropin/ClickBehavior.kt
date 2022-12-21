package com.mapbox.navigation.dropin

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class ClickBehavior<T> {

    private val _onViewClicked = MutableSharedFlow<T>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val onViewClicked = _onViewClicked.asSharedFlow()

    fun onClicked(value: T) {
        _onViewClicked.tryEmit(value)
    }
}
