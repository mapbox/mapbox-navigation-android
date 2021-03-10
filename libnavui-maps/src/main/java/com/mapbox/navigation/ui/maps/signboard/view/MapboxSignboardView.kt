package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import java.io.ByteArrayInputStream

/**
 * Default Signboard View that renders snapshot based on [SignboardState]
 */
class MapboxSignboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Invoke to render the signboard based on data or error conditions.
     */
    fun render(result: Expected<SignboardValue, SignboardError>) {
        when (result) {
            is Expected.Success -> {
                val signboard = renderSignboard(result.value)
                visibility = if (signboard != null) {
                    setImageBitmap(signboard)
                    VISIBLE
                } else {
                    setImageBitmap(null)
                    GONE
                }
            }
            is Expected.Failure -> {
                setImageBitmap(null)
                visibility = GONE
            }
        }
    }

    private fun renderSignboard(value: SignboardValue): Bitmap? {
        return when (value.bytes.isEmpty()) {
            true -> null
            else -> {
                val stream = ByteArrayInputStream(value.bytes)
                SvgUtil.renderAsBitmapWithWidth(stream, value.desiredSignboardWidth)
            }
        }
    }
}
