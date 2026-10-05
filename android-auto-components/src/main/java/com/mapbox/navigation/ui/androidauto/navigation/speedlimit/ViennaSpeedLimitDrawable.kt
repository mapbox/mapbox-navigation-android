package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Canvas

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
