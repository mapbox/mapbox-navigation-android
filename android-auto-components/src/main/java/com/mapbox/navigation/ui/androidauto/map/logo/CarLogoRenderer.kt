package com.mapbox.navigation.ui.androidauto.map.logo

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.androidauto.widgets.LogoWidget
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpScale

@OptIn(MapboxExperimental::class)
class CarLogoRenderer : MapboxCarMapObserver {

    private var logoWidget: LogoWidget? = null
    private var marginXPx = 0f
    private var marginYPx = 0f

    private companion object {
        // The logo bitmap follows the head unit density, so the margins do too.
        private const val MARGIN_X_DP = 16f
        private const val MARGIN_Y_DP = 8f
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        val scale = mapboxCarMapSurface.carContext.dpScale()
        marginXPx = MARGIN_X_DP * scale
        marginYPx = MARGIN_Y_DP * scale
        val logoWidget = LogoWidget(
            mapboxCarMapSurface.carContext,
            WidgetPosition {
                horizontalAlignment = WidgetPosition.Horizontal.RIGHT
                verticalAlignment = WidgetPosition.Vertical.BOTTOM
            },
            marginX = marginXPx,
            marginY = marginYPx,
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
                offsetX = -marginXPx - edgeInsets.right.toFloat()
                offsetY = -marginYPx - edgeInsets.bottom.toFloat()
            }.build(),
        )
    }
}
