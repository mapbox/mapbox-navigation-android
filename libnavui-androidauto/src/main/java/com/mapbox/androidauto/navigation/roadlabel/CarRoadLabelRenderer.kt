package com.mapbox.androidauto.navigation.roadlabel

import android.graphics.Color
import android.graphics.Rect
import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.internal.RendererUtils
import com.mapbox.androidauto.internal.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.navigation.MapUserStyleObserver
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShield

/**
 * This will show the current road name at the bottom center of the screen.
 *
 * In your [Screen], create an instance of this class and enable by
 * registering it to the [MapboxCarMap.registerObserver]. Disable by
 * removing the listener with [MapboxCarMap.unregisterObserver].
 */
@OptIn(MapboxExperimental::class)
class CarRoadLabelRenderer : MapboxCarMapObserver {

    private val roadLabelBitmapRenderer = CarRoadLabelBitmapRenderer()
    private val routeShieldApi = MapboxRouteShieldApi()
    private val mapUserStyleObserver = MapUserStyleObserver()
    private var styleLoadedListener: OnStyleLoadedListener? = null
    private var roadNameObserver: CarRoadNameObserver? = null
    private var roadLabelWidget: BitmapWidget? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface loaded")
        super.onAttached(mapboxCarMapSurface)
        val roadLabelWidget = BitmapWidget(
            RendererUtils.EMPTY_BITMAP,
            WidgetPosition(WidgetPosition.Horizontal.CENTER, WidgetPosition.Vertical.BOTTOM),
            marginY = 10f,
        ).also { roadLabelWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(roadLabelWidget)
        val carContext = mapboxCarMapSurface.carContext
        val roadNameObserver = object : CarRoadNameObserver(routeShieldApi, mapUserStyleObserver) {
            override fun onRoadUpdate(road: List<RoadComponent>, shields: List<RouteShield>) {
                val options = roadLabelOptions(carContext)
                val bitmap = roadLabelBitmapRenderer
                    .render(carContext.resources, road, shields, options)
                roadLabelWidget.updateBitmap(bitmap ?: RendererUtils.EMPTY_BITMAP)
            }
        }.also { roadNameObserver = it }
        styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached {
            val bitmap = roadLabelBitmapRenderer.render(
                carContext.resources,
                roadNameObserver.currentRoad,
                roadNameObserver.currentShields,
                roadLabelOptions(carContext)
            )
            roadLabelWidget.updateBitmap(bitmap ?: RendererUtils.EMPTY_BITMAP)
        }

        mapUserStyleObserver.onAttached(mapboxCarMapSurface)
        MapboxNavigationApp.registerObserver(roadNameObserver)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface detached")
        mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)
        roadNameObserver?.let { MapboxNavigationApp.unregisterObserver(it) }
        roadNameObserver = null
        routeShieldApi.cancel()
        mapUserStyleObserver.onDetached(mapboxCarMapSurface)
        roadLabelWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        roadLabelWidget = null
        super.onDetached(mapboxCarMapSurface)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        val translationX = (edgeInsets.left - edgeInsets.right) / 2
        roadLabelWidget?.setTranslation(translationX.toFloat(), -edgeInsets.bottom.toFloat())
    }

    private fun roadLabelOptions(carContext: CarContext): CarRoadLabelOptions =
        if (carContext.isDarkMode) {
            DARK_OPTIONS
        } else {
            LIGHT_OPTIONS
        }

    private companion object {

        private val DARK_OPTIONS = CarRoadLabelOptions.Builder()
            .shadowColor(null)
            .roundedLabelColor(Color.BLACK)
            .textColor(Color.WHITE)
            .build()

        private val LIGHT_OPTIONS = CarRoadLabelOptions.Builder()
            .roundedLabelColor(Color.WHITE)
            .textColor(Color.BLACK)
            .build()
    }
}
