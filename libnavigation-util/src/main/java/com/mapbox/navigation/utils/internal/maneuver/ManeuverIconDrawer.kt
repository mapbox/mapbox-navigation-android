package com.mapbox.navigation.utils.internal.maneuver

import android.graphics.Canvas
import android.graphics.PointF
import androidx.annotation.ColorInt

/**
 * Maneuver icon drawer. Used along with maneuver type and modifier. Used by [ManeuverIconHelper]
 * as a general interface to drawing junction views
 */
interface ManeuverIconDrawer {

    /**
     * Draws the maneuver icon
     *
     * @param canvas where the maneuver icon will be drawn
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
