package com.mapbox.navigation.ui.maps.guidance.junction.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue

/**
 * Default Junction View that renders junction.
 */
class MapboxJunctionView : AppCompatImageView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Invoke to render the signboard based on data or error conditions.
     */
    fun render(result: Expected<JunctionValue, JunctionError>) {
        when (result) {
            is Expected.Success -> {
                val signboard = renderJunction(result.value)
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

    private fun renderJunction(value: JunctionValue): Bitmap? {
        return when (value.bytes.isEmpty()) {
            true -> null
            else -> {
                BitmapFactory.decodeByteArray(value.bytes, 0, value.bytes.size)
            }
        }
    }
}
