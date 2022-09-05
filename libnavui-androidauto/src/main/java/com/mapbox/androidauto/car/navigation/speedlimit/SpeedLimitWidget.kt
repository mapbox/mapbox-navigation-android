package com.mapbox.androidauto.car.navigation.speedlimit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.base.speed.model.SpeedLimitSign

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
    marginX: Float = 14f,
    /**
     * The vertical margin of the widget relative to the map.
     */
    marginY: Float = 30f,
    /**
     * The initial sign format to use before speed limit info is available.
     */
    initialSignFormat: SpeedLimitSign = SpeedLimitSign.MUTCD,
) : BitmapWidget(
    drawSpeedLimitSign(speedLimit = null, speed = 0, initialSignFormat, warn = false),
    position, marginX, marginY,
) {

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
        ) return
        lastSpeedLimit = speedLimit
        lastSpeed = speed
        lastSignFormat = newSignFormat
        lastWarn = warn

        updateBitmap(drawSpeedLimitSign(speedLimit, speed, newSignFormat, warn))
    }

    internal companion object {
        private const val TAG = "SpeedLimitWidget"
        private const val WIDTH_VIENNA = 74
        private const val HEIGHT_VIENNA = 108
        private const val HEIGHT_SIGN = 67f
        private const val WIDTH_MUTCD = 77
        private const val HEIGHT_MUTCD = 115
        private const val STROKE = 1f
        private const val STROKE_SIGN_VIENNA = 4f
        private const val STROKE_SIGN_MUTCD = 2f
        private const val STROKE_PADDING = 2f
        private const val RADIUS_VIENNA = 25f
        private const val RADIUS_MUTCD = 9f
        private const val RADIUS_SHADOW = 12f
        private const val OFFSET_SHADOW = 8f
        private const val RADIUS_SHADOW_SMALL = 4f
        private const val OFFSET_SHADOW_SMALL = 2f
        private const val TITLE_1 = "SPEED"
        private const val TITLE_2 = "LIMIT"
        private const val SPEED_LIMIT_NO_DATA = "--"
        private val COLOR_BORDER = Color.parseColor("#CDD0D0")
        private val COLOR_RED = Color.parseColor("#BE3C30")
        private val COLOR_SHADOW = Color.parseColor("#1A000000")

        private val titlePaint = createTextPaint(Color.BLACK, 12f)
        private val speedLimitPaintVienna = createTextPaint(Color.BLACK, textSize = 22.5f)
        private val speedLimitPaintMutcd = createTextPaint(Color.BLACK, textSize = 27f)
        private val speedPaintNormal = createTextPaint(COLOR_RED, textSize = 22.5f)
        private val speedPaintWarning = createTextPaint(Color.WHITE, textSize = 22.5f)
        private val borderPaint = createBackgroundPaint(COLOR_BORDER)
        private val backgroundPaintNormal = createBackgroundPaint(Color.WHITE)
        private val backgroundPaintWarning = createBackgroundPaint(COLOR_RED)
        private val signBorderViennaPaint = createBackgroundPaint(COLOR_RED)
        private val signBorderMutcdPaint = createBackgroundPaint(COLOR_BORDER)
        private val titleRect1 = Rect().apply {
            titlePaint.getTextBounds(TITLE_1, 0, TITLE_1.length, this)
        }
        private val titleRect2 = Rect().apply {
            titlePaint.getTextBounds(TITLE_2, 0, TITLE_2.length, this)
        }
        private val speedLimitRect = Rect()
        private val speedRect = Rect()

        private val borderRectVienna = createFullRect(WIDTH_VIENNA, HEIGHT_VIENNA, inset = 0f)
        private val backgroundRectVienna = createFullRect(WIDTH_VIENNA, HEIGHT_VIENNA, STROKE)
        private val signBorderRectVienna = createSquare(WIDTH_VIENNA, STROKE)
        private val signBackgroundRectVienna =
            createSquare(WIDTH_VIENNA, inset = STROKE + STROKE_SIGN_VIENNA)
        private val borderRectMutcd = createFullRect(WIDTH_MUTCD, HEIGHT_MUTCD, inset = 0f)
        private val backgroundRectMutcd = createFullRect(WIDTH_MUTCD, HEIGHT_MUTCD, STROKE)
        private val signBorderRectMutcd =
            createRect(WIDTH_MUTCD, HEIGHT_SIGN, inset = STROKE + STROKE_PADDING)
        private val signBackgroundRectMutcd = createRect(
            WIDTH_MUTCD, HEIGHT_SIGN, inset = STROKE + STROKE_PADDING + STROKE_SIGN_MUTCD,
        )

        private fun drawSpeedLimitSign(
            speedLimit: Int?,
            speed: Int,
            signFormat: SpeedLimitSign,
            warn: Boolean,
        ): Bitmap {
            return when (signFormat) {
                SpeedLimitSign.MUTCD -> drawMutcdSpeedLimitSign(speedLimit, speed, warn)
                SpeedLimitSign.VIENNA -> drawViennaSpeedLimitSign(speedLimit, speed, warn)
            }
        }

        internal fun drawViennaSpeedLimitSign(speedLimit: Int?, speed: Int, warn: Boolean): Bitmap {
            logAndroidAuto(
                "$TAG drawViennaSpeedLimitSign: speedLimit = " +
                    "$speedLimit, speed = $speed, warn = $warn",
            )

            val canvasBitmap =
                Bitmap.createBitmap(WIDTH_VIENNA, HEIGHT_VIENNA, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)

            borderPaint.setShadowLayer(RADIUS_SHADOW_SMALL, 0f, OFFSET_SHADOW_SMALL, COLOR_SHADOW)
            canvas.drawRoundRect(borderRectVienna, RADIUS_VIENNA, RADIUS_VIENNA, borderPaint)
            borderPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
            canvas.drawRoundRect(borderRectVienna, RADIUS_VIENNA, RADIUS_VIENNA, borderPaint)
            canvas.drawRoundRect(
                backgroundRectVienna, RADIUS_VIENNA - STROKE, RADIUS_VIENNA - STROKE,
                if (warn) backgroundPaintWarning else backgroundPaintNormal,
            )

            val radiusSignBorder = RADIUS_VIENNA - STROKE
            if (warn) {
                signBorderViennaPaint.setShadowLayer(
                    RADIUS_SHADOW_SMALL, 0f, OFFSET_SHADOW_SMALL, COLOR_SHADOW,
                )
                canvas.drawRoundRect(
                    signBorderRectVienna, radiusSignBorder, radiusSignBorder, signBorderViennaPaint,
                )
                signBorderViennaPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
            } else {
                signBorderViennaPaint.clearShadowLayer()
            }
            canvas.drawRoundRect(
                signBorderRectVienna, radiusSignBorder, radiusSignBorder, signBorderViennaPaint,
            )
            canvas.drawRoundRect(
                signBackgroundRectVienna, radiusSignBorder - STROKE_SIGN_VIENNA,
                radiusSignBorder - STROKE_SIGN_VIENNA, backgroundPaintNormal,
            )

            val speedLimitText = speedLimit?.toString() ?: SPEED_LIMIT_NO_DATA
            speedLimitPaintVienna.getTextBounds(
                speedLimitText, 0, speedLimitText.length, speedLimitRect,
            )
            val speedLimitY = WIDTH_VIENNA / 2 - RADIUS_SHADOW - speedLimitRect.exactCenterY()
            canvas.drawText(speedLimitText, WIDTH_VIENNA / 2f, speedLimitY, speedLimitPaintVienna)

            val speedText = speed.toString()
            val speedPaint = if (warn) speedPaintWarning else speedPaintNormal
            speedPaint.getTextBounds(speedText, 0, speedText.length, speedRect)
            val speedY = signBorderRectVienna.bottom + 16 - speedRect.exactCenterY()
            canvas.drawText(speedText, WIDTH_VIENNA / 2f, speedY, speedPaint)

            return canvasBitmap
        }

        internal fun drawMutcdSpeedLimitSign(speedLimit: Int?, speed: Int, warn: Boolean): Bitmap {
            logAndroidAuto(
                "$TAG drawMutcdSpeedLimitSign: speedLimit = " +
                    "$speedLimit, speed = $speed, warn = $warn",
            )

            val canvasBitmap =
                Bitmap.createBitmap(WIDTH_MUTCD, HEIGHT_MUTCD, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)

            borderPaint.setShadowLayer(RADIUS_SHADOW_SMALL, 0f, OFFSET_SHADOW_SMALL, COLOR_SHADOW)
            canvas.drawRoundRect(borderRectMutcd, RADIUS_MUTCD, RADIUS_MUTCD, borderPaint)
            borderPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
            canvas.drawRoundRect(borderRectMutcd, RADIUS_MUTCD, RADIUS_MUTCD, borderPaint)
            canvas.drawRoundRect(
                backgroundRectMutcd, RADIUS_MUTCD - STROKE, RADIUS_MUTCD - STROKE,
                if (warn) backgroundPaintWarning else backgroundPaintNormal,
            )

            val radiusSignBorder = RADIUS_MUTCD - STROKE - STROKE_PADDING
            canvas.drawRoundRect(
                signBorderRectMutcd, radiusSignBorder, radiusSignBorder, signBorderMutcdPaint,
            )
            canvas.drawRoundRect(
                signBackgroundRectMutcd, radiusSignBorder - STROKE_SIGN_MUTCD,
                radiusSignBorder - STROKE_SIGN_MUTCD, backgroundPaintNormal,
            )

            val titleY1 = signBackgroundRectMutcd.top + 7.5f - titleRect1.exactCenterY()
            canvas.drawText(TITLE_1, WIDTH_MUTCD / 2f, titleY1, titlePaint)
            val titleY2 = signBackgroundRectMutcd.top + 19.5f - titleRect2.exactCenterY()
            canvas.drawText(TITLE_2, WIDTH_MUTCD / 2f, titleY2, titlePaint)

            val speedLimitText = speedLimit?.toString() ?: SPEED_LIMIT_NO_DATA
            speedLimitPaintMutcd.getTextBounds(
                speedLimitText, 0, speedLimitText.length, speedLimitRect,
            )
            val speedLimitY = signBackgroundRectMutcd.top + 41.5f - speedLimitRect.exactCenterY()
            canvas.drawText(speedLimitText, WIDTH_MUTCD / 2f, speedLimitY, speedLimitPaintVienna)

            val speedText = speed.toString()
            val speedPaint = if (warn) speedPaintWarning else speedPaintNormal
            speedPaint.getTextBounds(speedText, 0, speedText.length, speedRect)
            val speedY = signBorderRectMutcd.bottom + 14 - speedRect.exactCenterY()
            canvas.drawText(speedText, WIDTH_MUTCD / 2f, speedY, speedPaint)

            return canvasBitmap
        }

        private fun createBackgroundPaint(@ColorInt color: Int): Paint {
            return createPaint(color).apply {
                style = Paint.Style.FILL
            }
        }

        private fun createTextPaint(@ColorInt color: Int, textSize: Float): Paint {
            return createPaint(color).apply {
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
            }
        }

        private fun createPaint(@ColorInt color: Int): Paint {
            return Paint().apply {
                this.color = color
                isAntiAlias = true
            }
        }

        private fun createFullRect(width: Int, height: Int, inset: Float): RectF {
            return createRect(width, height - OFFSET_SHADOW - RADIUS_SHADOW, inset)
        }

        private fun createSquare(fullWidth: Int, inset: Float): RectF {
            return createRect(fullWidth, fullWidth - 2 * RADIUS_SHADOW, inset)
        }

        private fun createRect(fullWidth: Int, height: Float, inset: Float): RectF {
            return RectF(
                RADIUS_SHADOW + inset, inset, fullWidth - RADIUS_SHADOW - inset, height - inset,
            )
        }
    }
}
