package com.mapbox.navigation.ui.components.speedlimit.model

internal data class ViewConstraints(
    val startId: Int = 0,
    val startSide: Int = 0,
    val endId: Int = 0,
    val endSide: Int = 0,
    val viewId: Int = 0,
    val anchor: Int = 0,
    val shouldConnect: Boolean = true,
)
