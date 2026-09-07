package com.mapbox.navigation.ui.androidauto.navigation.lanes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.Step
import androidx.core.graphics.drawable.IconCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpToPx

/**
 * This class will take multiple [CarLaneIcon] objects which were created from
 * the [CarLaneMapper], and then generate a [CarIcon] that can be used by
 * android auto's [Step.Builder.setLanesImage] and [Step.Builder.addLane].
 */
internal class CarLaneIconRenderer(
    private val context: Context,
) {
    private val widthPx by lazy { context.dpToPx(LANE_IMAGE_WIDTH) }
    private val heightPx by lazy { context.dpToPx(LANE_IMAGE_HEIGHT) }
    private val carLaneBitmap by lazy {
        Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    }

    /**
     * Generate the [CarIcon] from a list of [CarLaneIcon]
     * This class
     */
    fun renderLanesIcons(
        carLaneIcons: List<CarLaneIcon>,
        @ColorInt background: Int,
        options: CarLaneIconOptions,
    ): CarIcon {
        carLaneBitmap.eraseColor(background)

        carLaneIcons.forEachIndexed { index, laneIcon ->
            val canvas = Canvas(carLaneBitmap)
            val vectorDrawable = VectorDrawableCompat.create(
                context.resources,
                laneIcon.laneIcon.drawableResId,
                options.theme(laneIcon.isActive),
            )!!
            val iconBounds = calculateBounds(index, carLaneIcons.size)
            vectorDrawable.bounds = iconBounds
            if (laneIcon.laneIcon.shouldFlip) {
                val pivotX = iconBounds.centerX().toFloat()
                canvas.scale(-1f, 1f, pivotX, 0f)
            }
            vectorDrawable.draw(canvas)
        }

        return CarIcon.Builder(
            IconCompat.createWithBitmap(carLaneBitmap),
        ).build()
    }

    private fun CarLaneIconOptions.theme(isActive: Boolean) = when {
        isActive -> activeTheme
        else -> notActiveTheme
    }

    private fun calculateBounds(laneNumber: Int, laneCount: Int): Rect {
        val iconWidth = widthPx / MAX_LANES
        val iconHalfWidth = iconWidth / 2
        val laneWidth = widthPx / (laneCount + 1)
        val centeringOffset = laneWidth * (laneNumber + 1)
        val left = centeringOffset - iconHalfWidth
        return Rect(left, 0, left + iconWidth, heightPx)
    }

    internal companion object {
        // The current implementation androidx.car.app.navigation.model.Step says it expects 294 x 44 dp
        // https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/car/app/app/src/main/java/androidx/car/app/navigation/model/Step.java#266
        // Although, a future version says 500 x 74 dp so this can be updated.
        private const val LANE_IMAGE_WIDTH = 294
        private const val LANE_IMAGE_HEIGHT = 44
        internal const val MAX_LANES = 6
    }
}
