package com.mapbox.navigation.dropin.map

import com.mapbox.geojson.Point
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class MapClickBehavior {

    private val _mapClickBehavior = MutableSharedFlow<Point>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val mapClickBehavior = _mapClickBehavior.asSharedFlow()

    fun onMapClicked(point: Point) {
        _mapClickBehavior.tryEmit(point)
    }
}
