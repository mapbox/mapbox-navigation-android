package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect

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
