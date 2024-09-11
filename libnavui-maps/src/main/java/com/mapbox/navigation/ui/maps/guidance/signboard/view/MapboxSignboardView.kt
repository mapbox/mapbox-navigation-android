package com.mapbox.navigation.ui.maps.guidance.signboard.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue

/**
 * Default Signboard View that renders a signboard view.
 */
@UiThread
class MapboxSignboardView : AppCompatImageView {

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
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    /**
     * Invoke to render the signboard based on data or clear the view on error conditions.
     *
     * @param result type [Expected] which holds either the signboard or error
     */
    fun render(result: Expected<SignboardError, SignboardValue>) {
        result.fold(
            { // error
                setImageBitmap(null)
            },
            { value ->
                setImageBitmap(value.bitmap)
            },
        )
    }
}
