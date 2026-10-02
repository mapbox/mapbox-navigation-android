package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.navigation.MapUserStyleObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    private lateinit var scope: CoroutineScope
    private var roadNameObserver: CarRoadNameObserver? = null
    private var roadLabelWidget: BitmapWidget? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface loaded")
        super.onAttached(mapboxCarMapSurface)
        val roadLabelWidget = BitmapWidget(
            EMPTY_BITMAP,
            WidgetPosition {
                horizontalAlignment = WidgetPosition.Horizontal.CENTER
                verticalAlignment = WidgetPosition.Vertical.BOTTOM
                offsetY = -MARGIN_Y
            },
        ).also { roadLabelWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(roadLabelWidget)
        val carContext = mapboxCarMapSurface.carContext
        val roadNameObserver = object : CarRoadNameObserver(routeShieldApi, mapUserStyleObserver) {
            override fun onRoadUpdate(road: List<RoadComponent>, shields: List<RouteShield>) {
                val options = roadLabelOptions(carContext)
                val bitmap = roadLabelBitmapRenderer
                    .render(carContext.resources, road, shields, options)
                roadLabelWidget.updateBitmap(bitmap ?: EMPTY_BITMAP)
            }
        }.also { roadNameObserver = it }
        scope = MainScope()
        mapboxCarMapSurface.styleFlow().onEach {
            val bitmap = roadLabelBitmapRenderer.render(
                carContext.resources,
                roadNameObserver.currentRoad,
                roadNameObserver.currentShields,
                roadLabelOptions(carContext),
            )
            roadLabelWidget.updateBitmap(bitmap ?: EMPTY_BITMAP)
        }.launchIn(scope)

        mapUserStyleObserver.onAttached(mapboxCarMapSurface)
        MapboxNavigationApp.registerObserver(roadNameObserver)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface detached")
        scope.cancel()
        roadNameObserver?.let { MapboxNavigationApp.unregisterObserver(it) }
        roadNameObserver = null
        routeShieldApi.cancel()
        mapUserStyleObserver.onDetached(mapboxCarMapSurface)
        roadLabelWidget?.let { mapboxCarMapSurface.mapSurface.removeWidget(it) }
        roadLabelWidget = null
        super.onDetached(mapboxCarMapSurface)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        val currentPosition = roadLabelWidget?.getPosition() ?: return
        val transitOffsetX = (edgeInsets.left - edgeInsets.right) / 2
        roadLabelWidget?.setPosition(
            currentPosition.toBuilder()
                .apply {
                    offsetX = transitOffsetX.toFloat()
                    offsetY = -MARGIN_Y - edgeInsets.bottom.toFloat()
                }
                .build(),
        )
    }

    private fun roadLabelOptions(carContext: CarContext): CarRoadLabelOptions =
        if (carContext.isDarkMode) {
            DARK_OPTIONS
        } else {
            LIGHT_OPTIONS
        }

    private companion object {
        private const val MARGIN_Y = 10f

        private val DARK_OPTIONS = CarRoadLabelOptions.Builder()
            .shadowColor(null)
            .roundedLabelColor(Color.BLACK)
            .textColor(Color.WHITE)
            .build()

        private val LIGHT_OPTIONS = CarRoadLabelOptions.Builder()
            .roundedLabelColor(Color.WHITE)
            .textColor(Color.BLACK)
            .build()

        private val EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
        }
    }
}
