package com.mapbox.navigation.ui.androidauto.placeslistonmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpToPx

/**
 * Render bitmaps that can be shown as markers on the map.
 */
class PlaceMarkerRenderer(
    private val context: Context,
    @ColorInt
    private val background: Int = Color.TRANSPARENT,
) {
    var bitmap: Bitmap? = null

    fun renderMarker() = CarIcon.Builder(
        IconCompat.createWithBitmap(bitmap()),
    ).build()

    private fun bitmap(): Bitmap {
        return bitmap ?: renderBitmap()
    }

    private fun renderBitmap(): Bitmap {
        val vectorDrawable = VectorDrawableCompat.create(
            context.resources,
            MARKER_RESOURCE,
            ContextThemeWrapper(context, R.style.CarAppTheme).theme,
        )!!

        val px = context.dpToPx(MARKER_ICON_DIMEN_DP)
        val bitmap: Bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(background)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private companion object {
        private const val MARKER_ICON_DIMEN_DP = 64
        private val MARKER_RESOURCE: Int = R.drawable.ic_baseline_location
    }
}
