package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

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
