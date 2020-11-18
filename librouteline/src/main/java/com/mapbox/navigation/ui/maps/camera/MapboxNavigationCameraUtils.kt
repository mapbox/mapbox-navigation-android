package com.mapbox.navigation.ui.maps.camera

import android.content.res.Resources
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size

internal fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

internal fun convertScreenCenterOffsetToEdgeInsets(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)): EdgeInsets {
    val displayMetrics = Resources.getSystem().getDisplayMetrics()
    val scale = displayMetrics.density
    val scaledMapSize = Size(mapSize.width * scale, mapSize.height * scale)
    val mapCenterScreenCoordinate = ScreenCoordinate((scaledMapSize.width / 2).toDouble(), (scaledMapSize.height / 2).toDouble())
    val top = mapCenterScreenCoordinate.y + centerOffset.y
    val left = mapCenterScreenCoordinate.x + centerOffset.x
    return EdgeInsets(top, left, scaledMapSize.height - top, scaledMapSize.width - left)
}
