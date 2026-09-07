package com.mapbox.navigation.ui.androidauto.map.logo

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.androidauto.widgets.LogoWidget
import com.mapbox.maps.renderer.widget.WidgetPosition

@OptIn(MapboxExperimental::class)
class CarLogoRenderer : MapboxCarMapObserver {

    private var logoWidget: LogoWidget? = null

    private companion object {
        private const val MARGIN_X = 26f
        private const val MARGIN_Y = 10f
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        // todo logo constructor ignore offsets https://mapbox.atlassian.net/browse/MAPSAND-1544
        val logoWidget = LogoWidget(
            mapboxCarMapSurface.carContext,
            WidgetPosition {
                horizontalAlignment = WidgetPosition.Horizontal.RIGHT
                verticalAlignment = WidgetPosition.Vertical.BOTTOM
            },
            marginX = MARGIN_X,
            marginY = MARGIN_Y,
        ).also { logoWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(logoWidget)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logoWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        logoWidget = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        val currentPosition = logoWidget?.getPosition() ?: return
        logoWidget?.setPosition(
            currentPosition.toBuilder().apply {
                offsetX = -MARGIN_X - edgeInsets.right.toFloat()
                offsetY = -MARGIN_Y - edgeInsets.bottom.toFloat()
            }.build(),
        )
    }
}
