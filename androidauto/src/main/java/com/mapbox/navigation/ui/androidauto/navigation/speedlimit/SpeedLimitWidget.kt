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
    }

    fun update(speedLimit: Int?, speed: Int, signFormat: SpeedLimitSign?, threshold: Int) {
        val newSignFormat = signFormat ?: lastSignFormat
        val warn = speedLimit != null && speed - threshold >= speedLimit
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
        val warn = speedLimit != null && speed - threshold >= speedLimit
        if (lastSignFormat == newSignFormat && lastWarn == warn) return
        lastSignFormat = newSignFormat
        lastWarn = warn

        updateBitmap(bitmapRenderer.getBitmap(newSignFormat, speedLimit, speed, warn))
    }
}
