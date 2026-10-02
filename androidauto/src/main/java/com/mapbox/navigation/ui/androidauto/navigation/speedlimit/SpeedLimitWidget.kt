package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * Widget to display a speed limit sign on the map.
 */
@MapboxExperimental
class SpeedLimitWidget private constructor(
    initialSignFormat: SpeedLimitSign,
    private val bitmapRenderer: SpeedLimitBitmapRenderer,
    position: WidgetPosition,
) : BitmapWidget(
    bitmap = bitmapRenderer.getBitmap(initialSignFormat),
    originalPosition = position,
) {
    constructor(initialSignFormat: SpeedLimitSign = SpeedLimitSign.MUTCD) : this(
        initialSignFormat = initialSignFormat,
        bitmapRenderer = SpeedLimitBitmapRenderer(),
        position = WidgetPosition {
            horizontalAlignment = WidgetPosition.Horizontal.RIGHT
            verticalAlignment = WidgetPosition.Vertical.BOTTOM
            offsetX = -MARGIN_X
            offsetY = -MARGIN_Y
        },
    )

    private var lastSpeedLimit: Int? = null
    private var lastSpeed = 0
    private var lastSignFormat = initialSignFormat
    private var lastWarn = false

    internal companion object {
        internal const val MARGIN_X = 14f
        internal const val MARGIN_Y = 30f

        // Driving at exactly the speed limit plus the threshold is still allowed.
        internal fun shouldWarn(speedLimit: Int?, speed: Int, threshold: Int): Boolean =
            speedLimit != null && speed - threshold > speedLimit
    }

    fun update(speedLimit: Int?, speed: Int, signFormat: SpeedLimitSign?, threshold: Int) {
        val newSignFormat = signFormat ?: lastSignFormat
        val warn = shouldWarn(speedLimit, speed, threshold)
        if (lastSpeedLimit == speedLimit &&
            lastSpeed == speed &&
            lastSignFormat == newSignFormat &&
            lastWarn == warn
        ) {
            return
        }
        lastSpeedLimit = speedLimit
        lastSpeed = speed
        lastSignFormat = newSignFormat
        lastWarn = warn

        updateBitmap(bitmapRenderer.getBitmap(newSignFormat, speedLimit, speed, warn))
    }

    fun update(signFormat: SpeedLimitSign?, threshold: Int) {
        val speedLimit = lastSpeedLimit
        val speed = lastSpeed
        val newSignFormat = signFormat ?: lastSignFormat
        val warn = shouldWarn(speedLimit, speed, threshold)
        if (lastSignFormat == newSignFormat && lastWarn == warn) return
        lastSignFormat = newSignFormat
        lastWarn = warn

        updateBitmap(bitmapRenderer.getBitmap(newSignFormat, speedLimit, speed, warn))
    }
}
