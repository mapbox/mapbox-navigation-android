package com.mapbox.androidauto.car.map.widgets.logo

import android.content.Context
import android.graphics.BitmapFactory
import com.mapbox.androidauto.car.map.widgets.WidgetPosition
import com.mapbox.examples.androidauto.R
import com.mapbox.androidauto.car.map.widgets.ImageOverlayHost
import com.mapbox.androidauto.car.map.widgets.Margin

/**
 * Logo widget displays the Mapbox logo on the map.
 */
class LogoWidget(
    /**
     * Context of the app.
     */
    context: Context,
    /**
     * The position of the widget.
     */
    widgetPosition: WidgetPosition = WidgetPosition.BOTTOM_RIGHT,
    /**
     * The left margin of the widget relative to the map.
     */
    marginLeft: Float = 10f,
    /**
     * The top margin of the widget relative to the map.
     */
    marginTop: Float = 10f,
    /**
     * The right margin of the widget relative to the map.
     */
    marginRight: Float = 10f,
    /**
     * The bottom margin of the widget relative to the map.
     */
    marginBottom: Float = 10f
) {
    internal val host by lazy {
        ImageOverlayHost(
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.mapbox_logo_icon
            ),
            widgetPosition, Margin(marginLeft, marginTop, marginRight, marginBottom),
        )
    }

    companion object {
        /**
         * The layer ID of the logo widget layer.
         */
        const val LOGO_WIDGET_LAYER_ID = "LOGO_WIDGET_LAYER"
    }
}
