package com.mapbox.navigation.ui.maps.camera

import android.content.res.Resources
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size

internal fun getScaledEdgeInsets(edgeInsets: EdgeInsets): EdgeInsets {
    val displayMetrics = Resources.getSystem().getDisplayMetrics()
    val scale = displayMetrics.density
    return EdgeInsets(edgeInsets.top * scale, edgeInsets.left * scale, edgeInsets.bottom * scale, edgeInsets.right * scale)
}

internal fun shortestRotation(from: Double, to: Double): Double {
    return (to - from + 540) % 360 - 180
}

internal fun convertScreenCenterOffsetToEdgeInsets(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)): EdgeInsets {
    val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
    val top = mapCenterScreenCoordinate.y + centerOffset.y
    val left = mapCenterScreenCoordinate.x + centerOffset.x
    return getScaledEdgeInsets(EdgeInsets(top, left, mapSize.height - top, mapSize.width - left))
}
