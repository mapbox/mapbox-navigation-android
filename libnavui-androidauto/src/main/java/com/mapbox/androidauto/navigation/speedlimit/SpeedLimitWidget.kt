package com.mapbox.androidauto.navigation.speedlimit

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
    private val bitmapProvider: SpeedLimitBitmapProvider,
    position: WidgetPosition,
    marginX: Float,
    marginY: Float
) : BitmapWidget(
    bitmap = bitmapProvider.getBitmap(initialSignFormat),
    position,
    marginX,
    marginY
) {
    constructor(initialSignFormat: SpeedLimitSign = SpeedLimitSign.MUTCD) : this(
        initialSignFormat = initialSignFormat,
        bitmapProvider = SpeedLimitBitmapProvider(),
        position = WidgetPosition(WidgetPosition.Horizontal.RIGHT, WidgetPosition.Vertical.BOTTOM),
        marginX = 14f,
        marginY = 30f,
    )

    private var lastSpeedLimit: Int? = null
    private var lastSpeed = 0
    private var lastSignFormat = initialSignFormat
    private var lastWarn = false

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

        updateBitmap(bitmapProvider.getBitmap(newSignFormat, speedLimit, speed, warn))
    }

    fun update(signFormat: SpeedLimitSign?, threshold: Int) {
        val speedLimit = lastSpeedLimit
        val speed = lastSpeed
        val newSignFormat = signFormat ?: lastSignFormat
        val warn = speedLimit != null && speed - threshold >= speedLimit
        if (lastSignFormat == newSignFormat && lastWarn == warn) return
        lastSignFormat = newSignFormat
        lastWarn = warn

        updateBitmap(bitmapProvider.getBitmap(newSignFormat, speedLimit, speed, warn))
    }
}
