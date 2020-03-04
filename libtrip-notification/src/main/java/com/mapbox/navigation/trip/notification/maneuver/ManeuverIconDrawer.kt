package com.mapbox.navigation.trip.notification.maneuver

import android.graphics.Canvas
import android.graphics.PointF

interface ManeuverIconDrawer {
    fun drawManeuverIcon(
        canvas: Canvas,
        primaryColor: Int,
        secondaryColor: Int,
        size: PointF,
        roundaboutAngle: Float
    )
}
