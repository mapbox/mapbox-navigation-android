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
    @get:JvmSynthetic internal val marginXPx: Float,
    @get:JvmSynthetic internal val marginYPx: Float,
) : BitmapWidget(
    bitmap = bitmapRenderer.getBitmap(initialSignFormat),
    originalPosition = WidgetPosition {
        horizontalAlignment = WidgetPosition.Horizontal.RIGHT
        verticalAlignment = WidgetPosition.Vertical.BOTTOM
        offsetX = -marginXPx
        offsetY = -marginYPx
    },
) {
    /**
     * The sign and its margins are drawn at their size in pixels. The SDK's own speed limit
     * renderer scales them to the head unit density.
     */
    constructor(initialSignFormat: SpeedLimitSign = SpeedLimitSign.MUTCD) : this(
        initialSignFormat = initialSignFormat,
        bitmapRenderer = SpeedLimitBitmapRenderer(),
        marginXPx = MARGIN_X_DP,
        marginYPx = MARGIN_Y_DP,
    )

    private var lastSpeedLimit: Int? = null
    private var lastSpeed = 0
    private var lastSignFormat = initialSignFormat
    private var lastWarn = false

    internal companion object {
        private const val MARGIN_X_DP = 14f
        private const val MARGIN_Y_DP = 30f

        /**
         * @param scale the number of pixels in one dp on the map surface. The sign and its
         * margins are designed in dp.
         */
        @JvmSynthetic
        internal fun scaled(initialSignFormat: SpeedLimitSign, scale: Float) = SpeedLimitWidget(
            initialSignFormat = initialSignFormat,
            bitmapRenderer = SpeedLimitBitmapRenderer(scale),
            marginXPx = MARGIN_X_DP * scale,
            marginYPx = MARGIN_Y_DP * scale,
        )

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
