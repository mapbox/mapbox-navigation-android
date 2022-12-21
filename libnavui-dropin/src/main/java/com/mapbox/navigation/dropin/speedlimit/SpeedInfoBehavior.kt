package com.mapbox.navigation.dropin.speedlimit

import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class SpeedInfoBehavior {

    private val _speedInfoClickBehavior = MutableSharedFlow<SpeedInfoValue?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val speedInfoClickBehavior = _speedInfoClickBehavior.asSharedFlow()

    fun onSpeedInfoClicked(speedInfo: SpeedInfoValue?) {
        _speedInfoClickBehavior.tryEmit(speedInfo)
    }
}
