package com.mapbox.androidauto.map.logo

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.androidauto.widgets.LogoWidget
import com.mapbox.maps.renderer.widget.WidgetPosition

@OptIn(MapboxExperimental::class)
class CarLogoSurfaceRenderer : MapboxCarMapObserver {

    private var logoWidget: LogoWidget? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        val logoWidget = LogoWidget(
            mapboxCarMapSurface.carContext,
            WidgetPosition(WidgetPosition.Horizontal.RIGHT, WidgetPosition.Vertical.BOTTOM),
            marginX = 26f,
            marginY = 10f,
        ).also { logoWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(logoWidget)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logoWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        logoWidget = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        logoWidget?.setTranslation(-edgeInsets.right.toFloat(), -edgeInsets.bottom.toFloat())
    }
}
