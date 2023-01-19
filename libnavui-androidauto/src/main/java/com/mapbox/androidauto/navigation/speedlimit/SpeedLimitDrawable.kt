package com.mapbox.androidauto.navigation.speedlimit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

internal abstract class SpeedLimitDrawable : Drawable() {
    var speedLimit: Int? = null
    var speed: Int = 0
    var warn: Boolean = false

    protected val titlePaint = createTextPaint(Color.BLACK, 12f)
    protected val speedLimitPaintVienna = createTextPaint(Color.BLACK, textSize = 22.5f)
    protected val speedPaintNormal = createTextPaint(COLOR_RED, textSize = 22.5f)
    protected val speedPaintWarning = createTextPaint(Color.WHITE, textSize = 22.5f)
    protected val borderPaint = createBackgroundPaint(COLOR_BORDER)
    protected val backgroundPaintNormal = createBackgroundPaint(Color.WHITE)
    protected val backgroundPaintWarning = createBackgroundPaint(COLOR_RED)
    protected val speedLimitRect = Rect()
    protected val speedRect = Rect()

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    companion object {
        const val STROKE = 1f
        const val RADIUS_SHADOW = 12f
        const val OFFSET_SHADOW = 8f
        const val RADIUS_SHADOW_SMALL = 4f
        const val OFFSET_SHADOW_SMALL = 2f
        const val BYTES_PER_ARGB_8888_PIXEL = 4
        const val SPEED_LIMIT_NO_DATA = "--"
        val COLOR_BORDER = Color.parseColor("#CDD0D0")
        val COLOR_RED = Color.parseColor("#BE3C30")
        val COLOR_SHADOW = Color.parseColor("#1A000000")

        fun createBackgroundPaint(@ColorInt color: Int): Paint {
            return createPaint(color).apply {
                style = Paint.Style.FILL
            }
        }

        fun createTextPaint(@ColorInt color: Int, textSize: Float): Paint {
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

        fun createFullRect(width: Int, height: Int, inset: Float): RectF {
            return createRect(width, height - OFFSET_SHADOW - RADIUS_SHADOW, inset)
        }

        fun createSquare(fullWidth: Int, inset: Float): RectF {
            return createRect(fullWidth, fullWidth - 2 * RADIUS_SHADOW, inset)
        }

        fun createRect(fullWidth: Int, height: Float, inset: Float): RectF {
            return RectF(
                RADIUS_SHADOW + inset,
                inset,
                fullWidth - RADIUS_SHADOW - inset,
                height - inset,
            )
        }
    }
}

internal class ViennaSpeedLimitDrawable : SpeedLimitDrawable() {
    companion object {
        const val WIDTH = 74
        const val HEIGHT = 108
        const val BITMAP_BYTE_SIZE: Long =
            (WIDTH * HEIGHT * BYTES_PER_ARGB_8888_PIXEL).toLong()

        const val RADIUS = 25f
        const val STROKE_SIGN = 4f
    }

    private val signBorderViennaPaint = createBackgroundPaint(COLOR_RED)
    private val borderRectVienna = createFullRect(WIDTH, HEIGHT, inset = 0f)
    private val backgroundRectVienna = createFullRect(WIDTH, HEIGHT, STROKE)
    private val signBorderRectVienna = createSquare(WIDTH, STROKE)
    private val signBackgroundRectVienna =
        createSquare(WIDTH, inset = STROKE + STROKE_SIGN)

    override fun draw(canvas: Canvas) {
        drawShadows(canvas)
        drawBackground(canvas)
        drawSignBackground(canvas)
        drawSignSpeedLimitText(canvas)
        drawCurrentSpeedText(canvas)
    }

    private fun drawShadows(canvas: Canvas) {
        borderPaint.setShadowLayer(RADIUS_SHADOW_SMALL, 0f, OFFSET_SHADOW_SMALL, COLOR_SHADOW)
        canvas.drawRoundRect(borderRectVienna, RADIUS, RADIUS, borderPaint)
        borderPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRoundRect(borderRectVienna, RADIUS, RADIUS, borderPaint)
        canvas.drawRoundRect(
            backgroundRectVienna,
            RADIUS - STROKE,
            RADIUS - STROKE,
            if (warn) backgroundPaintWarning else backgroundPaintNormal,
        )
    }

    private fun drawSignBackground(canvas: Canvas) {
        val radiusSignBorder = RADIUS - STROKE
        if (warn) {
            signBorderViennaPaint.setShadowLayer(
                RADIUS_SHADOW_SMALL,
                0f,
                OFFSET_SHADOW_SMALL,
                COLOR_SHADOW,
            )
            canvas.drawRoundRect(
                signBorderRectVienna,
                radiusSignBorder,
                radiusSignBorder,
                signBorderViennaPaint,
            )
            signBorderViennaPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
        } else {
            signBorderViennaPaint.clearShadowLayer()
        }
        canvas.drawRoundRect(
            signBorderRectVienna,
            radiusSignBorder,
            radiusSignBorder,
            signBorderViennaPaint,
        )
        canvas.drawRoundRect(
            signBackgroundRectVienna,
            radiusSignBorder - STROKE_SIGN,
            radiusSignBorder - STROKE_SIGN,
            backgroundPaintNormal,
        )
    }

    private fun drawSignSpeedLimitText(canvas: Canvas) {
        val speedLimitText = speedLimit?.toString() ?: SPEED_LIMIT_NO_DATA
        speedLimitPaintVienna.getTextBounds(
            speedLimitText,
            0,
            speedLimitText.length,
            speedLimitRect,
        )
        val speedLimitY = WIDTH / 2 - RADIUS_SHADOW - speedLimitRect.exactCenterY()
        canvas.drawText(speedLimitText, WIDTH / 2f, speedLimitY, speedLimitPaintVienna)
    }

    private fun drawCurrentSpeedText(canvas: Canvas) {
        val speedText = speed.toString()
        val speedPaint = if (warn) speedPaintWarning else speedPaintNormal
        speedPaint.getTextBounds(speedText, 0, speedText.length, speedRect)
        val speedY = signBorderRectVienna.bottom + 16 - speedRect.exactCenterY()
        canvas.drawText(speedText, WIDTH / 2f, speedY, speedPaint)
    }
}

internal class MutcdSpeedLimitDrawable : SpeedLimitDrawable() {
    companion object {
        const val WIDTH = 77
        const val HEIGHT = 115
        const val BITMAP_BYTE_SIZE: Long =
            (WIDTH * HEIGHT * BYTES_PER_ARGB_8888_PIXEL).toLong()
        const val HEIGHT_SIGN = 67f
        const val RADIUS = 9f
        const val STROKE_SIGN = 2f
        const val TITLE_1 = "SPEED"
        const val TITLE_2 = "LIMIT"
        const val STROKE_PADDING = 2f
    }

    private val speedLimitTextPaint = createTextPaint(Color.BLACK, textSize = 27f)
    private val signBorderPaint = createBackgroundPaint(COLOR_BORDER)

    private val titleRect1 = Rect().apply {
        titlePaint.getTextBounds(TITLE_1, 0, TITLE_1.length, this)
    }
    private val titleRect2 = Rect().apply {
        titlePaint.getTextBounds(TITLE_2, 0, TITLE_2.length, this)
    }
    private val borderRect = createFullRect(WIDTH, HEIGHT, inset = 0f)
    private val backgroundRect = createFullRect(WIDTH, HEIGHT, STROKE)
    private val signBorderRect =
        createRect(WIDTH, HEIGHT_SIGN, inset = STROKE + STROKE_PADDING)
    private val signBackgroundRect = createRect(
        WIDTH,
        HEIGHT_SIGN,
        inset = STROKE + STROKE_PADDING + STROKE_SIGN,
    )

    override fun draw(canvas: Canvas) {
        drawShadows(canvas)
        drawBackground(canvas)
        drawSignBackground(canvas)
        drawSignSpeedLimitText(canvas)
        drawCurrentSpeedText(canvas)
    }

    private fun drawShadows(canvas: Canvas) {
        borderPaint.setShadowLayer(RADIUS_SHADOW_SMALL, 0f, OFFSET_SHADOW_SMALL, COLOR_SHADOW)
        canvas.drawRoundRect(borderRect, RADIUS, RADIUS, borderPaint)
        borderPaint.setShadowLayer(RADIUS_SHADOW, 0f, OFFSET_SHADOW, COLOR_SHADOW)
        canvas.drawRoundRect(borderRect, RADIUS, RADIUS, borderPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRoundRect(
            backgroundRect,
            RADIUS - STROKE,
            RADIUS - STROKE,
            if (warn) backgroundPaintWarning else backgroundPaintNormal,
        )
    }

    private fun drawSignBackground(canvas: Canvas) {
        val radiusSignBorder = RADIUS - STROKE - STROKE_PADDING
        canvas.drawRoundRect(
            signBorderRect,
            radiusSignBorder,
            radiusSignBorder,
            signBorderPaint,
        )
        canvas.drawRoundRect(
            signBackgroundRect,
            radiusSignBorder - STROKE_SIGN,
            radiusSignBorder - STROKE_SIGN,
            backgroundPaintNormal,
        )

        val titleY1 = signBackgroundRect.top + 7.5f - titleRect1.exactCenterY()
        canvas.drawText(TITLE_1, WIDTH / 2f, titleY1, titlePaint)
        val titleY2 = signBackgroundRect.top + 19.5f - titleRect2.exactCenterY()
        canvas.drawText(TITLE_2, WIDTH / 2f, titleY2, titlePaint)
    }

    private fun drawSignSpeedLimitText(canvas: Canvas) {
        val speedLimitText = speedLimit?.toString() ?: SPEED_LIMIT_NO_DATA
        speedLimitTextPaint.getTextBounds(
            speedLimitText,
            0,
            speedLimitText.length,
            speedLimitRect,
        )
        val speedLimitY = signBackgroundRect.top + 41.5f - speedLimitRect.exactCenterY()
        canvas.drawText(speedLimitText, WIDTH / 2f, speedLimitY, speedLimitPaintVienna)
    }

    private fun drawCurrentSpeedText(canvas: Canvas) {
        val speedText = speed.toString()
        val speedPaint = if (warn) speedPaintWarning else speedPaintNormal
        speedPaint.getTextBounds(speedText, 0, speedText.length, speedRect)
        val speedY = signBorderRect.bottom + 14 - speedRect.exactCenterY()
        canvas.drawText(speedText, WIDTH / 2f, speedY, speedPaint)
    }
}
