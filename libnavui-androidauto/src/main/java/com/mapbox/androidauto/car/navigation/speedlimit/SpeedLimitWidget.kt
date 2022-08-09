package com.mapbox.androidauto.car.navigation.speedlimit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.WidgetPosition

/**
 * Widget to display a speed limit sign on the map.
 */
@OptIn(MapboxExperimental::class)
class SpeedLimitWidget(
    /**
     * The position of the widget.
     */
    position: WidgetPosition = WidgetPosition(
        vertical = WidgetPosition.Vertical.BOTTOM,
        horizontal = WidgetPosition.Horizontal.RIGHT,
    ),
    /**
     * The horizontal margin of the widget relative to the map.
     */
    marginX: Float = 26f,
    /**
     * The vertical margin of the widget relative to the map.
     */
    marginY: Float = 50f,
) : BitmapWidget(drawSpeedLimitSign(speedLimit = null, speed = 0), position, marginX, marginY) {

    private var lastSpeedLimit: Int? = null
    private var lastSpeed = 0

    fun update(speedLimit: Int?, speed: Int) {
        if (lastSpeedLimit == speedLimit && lastSpeed == speed) return
        lastSpeedLimit = speedLimit
        lastSpeed = speed

        updateBitmap(drawSpeedLimitSign(speedLimit, speed))
    }

    internal companion object {
        private const val TAG = "SpeedLimitWidget"
        private const val WIDTH = 55
        private const val HEIGHT = 98
        private const val PADDING = 3f
        private const val STROKE = 2f
        private const val RADIUS = 10f
        private const val SIGN_HEIGHT = 57
        private const val TITLE_1 = "SPEED"
        private const val TITLE_2 = "LIMIT"
        private const val SPEED_LIMIT_NO_DATA = "--"
        private val COLOR_NORMAL = Color.parseColor("#4F4F4F")

        private val titlePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        private val speedLimitPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 27f
            textAlign = Paint.Align.CENTER
        }
        private val speedPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = 22.5f
            textAlign = Paint.Align.CENTER
        }
        private val signPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        private val borderPaint = Paint().apply {
            color = Color.parseColor("#CDD0D0")
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = STROKE
        }
        private val backgroundPaint = Paint().apply {
            isAntiAlias = true
        }
        private val titleRect1 = Rect().apply {
            titlePaint.getTextBounds(TITLE_1, 0, TITLE_1.length, this)
        }
        private val titleRect2 = Rect().apply {
            titlePaint.getTextBounds(TITLE_2, 0, TITLE_2.length, this)
        }
        private val speedLimitRect = Rect()
        private val speedRect = Rect()
        private val signRect = RectF(
            PADDING + STROKE / 2,
            PADDING + STROKE / 2,
            WIDTH - PADDING - STROKE / 2,
            PADDING + STROKE * 1.5f + SIGN_HEIGHT,
        )
        private val backgroundRect = RectF(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())

        internal fun drawSpeedLimitSign(speedLimit: Int?, speed: Int): Bitmap {
            logAndroidAuto("$TAG drawSpeedLimitSign: speedLimit = $speedLimit, speed = $speed")

            val speedLimitText = if (speedLimit == null) {
                backgroundPaint.color = COLOR_NORMAL
                SPEED_LIMIT_NO_DATA
            } else {
                backgroundPaint.color = if (speed > speedLimit) Color.RED else COLOR_NORMAL
                speedLimit.toString()
            }
            speedLimitPaint.getTextBounds(speedLimitText, 0, speedLimitText.length, speedLimitRect)
            val titlePadding = (SIGN_HEIGHT / 2f - titleRect1.height() - titleRect2.height()) / 3
            val speedText = speed.toString()
            speedPaint.getTextBounds(speedText, 0, speedText.length, speedRect)

            val canvasBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawRoundRect(backgroundRect, RADIUS, RADIUS, backgroundPaint)
            canvas.drawRoundRect(signRect, RADIUS - PADDING, RADIUS - PADDING, signPaint)
            canvas.drawRoundRect(signRect, RADIUS - PADDING, RADIUS - PADDING, borderPaint)

            val titleY1 = PADDING + STROKE + titlePadding
            canvas.drawText(TITLE_1, WIDTH / 2f, titleY1 - titleRect1.top, titlePaint)

            val titleY2 = titleY1 + titleRect1.height() + titlePadding
            canvas.drawText(TITLE_2, WIDTH / 2f, titleY2 - titleRect2.top, titlePaint)

            val speedLimitY = PADDING + STROKE + SIGN_HEIGHT * 0.75f - speedLimitRect.centerY()
            canvas.drawText(speedLimitText, WIDTH / 2f, speedLimitY, speedLimitPaint)

            val speedY = (PADDING + SIGN_HEIGHT + HEIGHT) / 2 + STROKE - speedRect.centerY()
            canvas.drawText(speedText, WIDTH / 2f, speedY, speedPaint)

            return canvasBitmap
        }
    }
}
