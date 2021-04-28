package com.mapbox.navigation.ui.maps.guidance.junction.view

import android.content.Context
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
     * Invoke to render the junction based on data or clear the view on error conditions.
     *
     * @param result type [Expected] which holds either the junction or error
     */
    fun render(result: Expected<JunctionValue, JunctionError>) {
        when (result) {
            is Expected.Success -> {
                setImageBitmap(result.value.bitmap)
            }
            is Expected.Failure -> {
                setImageBitmap(null)
            }
        }
    }
}
