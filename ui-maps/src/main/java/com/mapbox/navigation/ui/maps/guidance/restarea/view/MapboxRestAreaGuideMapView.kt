package com.mapbox.navigation.ui.maps.guidance.restarea.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapError
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapValue

/**
 * Default rendering of service/parking area guide map.
 */
class MapboxRestAreaGuideMapView : AppCompatImageView {

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
     * Invoke to render the service/parking area guide map based on data or clear the
     * view on error conditions.
     *
     * @param result type [Expected] which holds either the sapa guide map or error
     */
    fun render(result: Expected<RestAreaGuideMapError, RestAreaGuideMapValue>) {
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
