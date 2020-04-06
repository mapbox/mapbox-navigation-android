package com.mapbox.navigation.trip.notification.maneuver

import android.graphics.Canvas
import android.graphics.PointF
import androidx.annotation.ColorInt

/**
 * Maneuver icon drawer
 */
interface ManeuverIconDrawer {
    /**
     * Draw Maneuver icon
     *
     * @param canvas where Maneuver will be drawn
     * @param primaryColor primary color
     * @param secondaryColor secondary color
     * @param size PointF
     * @param roundaboutAngle Float
     */
    fun drawManeuverIcon(
        canvas: Canvas,
        @ColorInt primaryColor: Int,
        @ColorInt secondaryColor: Int,
        size: PointF,
        roundaboutAngle: Float
    )
}
