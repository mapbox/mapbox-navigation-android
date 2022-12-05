package com.mapbox.navigation.qa_test_app.view.util

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.drawToBitmap
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutMapMarkerWithTextBinding

internal class IconFactory(private val context: Context) {

    /**
     * Pin with Text
     * @see [R.layout.layout_map_marker_with_text]
     */
    fun pinIconWithText(
        text: String,
        @ColorInt tintColor: Int? = null,
        @ColorInt textColor: Int? = null
    ): Bitmap {
        val binding = LayoutMapMarkerWithTextBinding.inflate(LayoutInflater.from(context))
        tintColor?.also { color ->
            DrawableCompat.setTint(DrawableCompat.wrap(binding.image.drawable), color)
        }
        binding.text.text = text
        textColor?.also { binding.text.setTextColor(it) }
        return binding.root.run {
            // Cause the view to re-layout
            measure(measuredWidth, measuredHeight)
            layout(0, 0, measuredWidth, measuredHeight)
            drawToBitmap()
        }
    }
}
