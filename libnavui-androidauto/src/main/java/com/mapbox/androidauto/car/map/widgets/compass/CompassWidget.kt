package com.mapbox.androidauto.car.map.widgets.compass

import android.content.Context
import android.graphics.BitmapFactory
import com.mapbox.androidauto.car.map.widgets.WidgetPosition
import com.mapbox.examples.androidauto.R
import com.mapbox.androidauto.car.map.widgets.ImageOverlayHost
import com.mapbox.androidauto.car.map.widgets.Margin

/**
 * A widget to show the compass on the map.
 */
class CompassWidget(
    /**
     * Context of the app.
     */
    context: Context,
    /**
     * The position of the widget.
     */
    widgetPosition: WidgetPosition = WidgetPosition.TOP_RIGHT,
    /**
     * The left margin of the widget relative to the map.
     */
    marginLeft: Float = 10f,
    /**
     * The top margin of the widget relative to the map.
     */
    marginTop: Float = 130f,
    /**
     * The right margin of the widget relative to the map.
     */
    marginRight: Float = 10f,
    /**
     * The bottom margin of the widget relative to the map.
     */
    marginBottom: Float = 10f
) {
    internal val host = ImageOverlayHost(
        BitmapFactory.decodeResource(
            context.resources,
            R.drawable.mapbox_compass_icon
        ),
        widgetPosition, Margin(marginLeft, marginTop, marginRight, marginBottom),
    )

    /**
     * Update the compass bearing.
     */
    fun updateBearing(bearing: Float) {
        host.rotate(-bearing)
    }

    companion object {
        /**
         * The layer ID of the compass widget layer.
         */
        const val COMPASS_WIDGET_LAYER_ID = "COMPASS_WIDGET_LAYER"
    }
}
