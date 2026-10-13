package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.graphics.Color
import android.graphics.Rect
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.core.graphics.createBitmap
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
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpScale
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.navigation.MapUserStyleObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

/**
 * This will show the current road name at the bottom center of the screen. The label is kept out
 * of the bottom corners of the visible area, where the Mapbox logo and the speed limit sign are
 * drawn, and a road name that does not fit is ellipsized.
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
    private var carContext: CarContext? = null
    private var dpScale = 1f
    private var marginYPx = 0f
    private var maxLabelWidthPx = Int.MAX_VALUE

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface loaded")
        super.onAttached(mapboxCarMapSurface)
        carContext = mapboxCarMapSurface.carContext
        dpScale = mapboxCarMapSurface.carContext.dpScale()
        marginYPx = MARGIN_Y_DP * dpScale
        maxLabelWidthPx = Int.MAX_VALUE
        val roadLabelWidget = BitmapWidget(
            EMPTY_BITMAP,
            WidgetPosition {
                horizontalAlignment = WidgetPosition.Horizontal.CENTER
                verticalAlignment = WidgetPosition.Vertical.BOTTOM
                offsetY = -marginYPx
            },
        ).also { roadLabelWidget = it }
        mapboxCarMapSurface.mapSurface.addWidget(roadLabelWidget)
        val roadNameObserver = object : CarRoadNameObserver(routeShieldApi, mapUserStyleObserver) {
            override fun onRoadUpdate(road: List<RoadComponent>, shields: List<RouteShield>) {
                updateLabel(road, shields)
            }
        }.also { roadNameObserver = it }
        scope = MainScope()
        mapboxCarMapSurface.styleFlow().onEach {
            updateLabel(roadNameObserver.currentRoad, roadNameObserver.currentShields)
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
        carContext = null
        super.onDetached(mapboxCarMapSurface)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        val currentPosition = roadLabelWidget?.getPosition() ?: return
        val transitOffsetX = (edgeInsets.left - edgeInsets.right) / 2
        roadLabelWidget?.setPosition(
            currentPosition.toBuilder()
                .apply {
                    offsetX = transitOffsetX.toFloat()
                    offsetY = -marginYPx - edgeInsets.bottom.toFloat()
                }
                .build(),
        )
        val maxLabelWidthPx = maxLabelWidthPx(visibleArea)
        if (this.maxLabelWidthPx != maxLabelWidthPx) {
            this.maxLabelWidthPx = maxLabelWidthPx
            roadNameObserver?.let { updateLabel(it.currentRoad, it.currentShields) }
        }
    }

    /**
     * The label is centered in the visible area, so the same width is reserved at both sides
     * to keep the bottom corners free. When the space left is too small for a useful label,
     * the limit is 0 and the label is hidden rather than drawn over the corners.
     */
    private fun maxLabelWidthPx(visibleArea: Rect): Int {
        if (visibleArea.isEmpty) return Int.MAX_VALUE
        val labelWidth = visibleArea.width() - 2 * CORNER_RESERVE_DP * dpScale
        return if (labelWidth < MIN_LABEL_WIDTH_DP * dpScale) 0 else labelWidth.roundToInt()
    }

    private fun updateLabel(road: List<RoadComponent>, shields: List<RouteShield>) {
        val carContext = carContext ?: return
        val bitmap = roadLabelBitmapRenderer.render(
            carContext.resources,
            road,
            shields,
            roadLabelOptions(carContext),
            maxLabelWidthPx,
        )
        roadLabelWidget?.updateBitmap(bitmap ?: EMPTY_BITMAP)
    }

    private fun roadLabelOptions(carContext: CarContext): CarRoadLabelOptions =
        if (carContext.isDarkMode) {
            DARK_OPTIONS
        } else {
            LIGHT_OPTIONS
        }

    private companion object {
        // The label bitmap has a transparent padding of 10dp, so the card bottom is 32dp above
        // the visible area bottom. This is above the Mapbox logo in the bottom corner.
        private const val MARGIN_Y_DP = 22f

        // The Mapbox logo (85dp wide) with its margin, and the speed limit sign, fit in this
        // width at the bottom corners of the visible area.
        private const val CORNER_RESERVE_DP = 110f

        // A narrower space between the corners hides the label.
        private const val MIN_LABEL_WIDTH_DP = 80f

        private val DARK_OPTIONS = CarRoadLabelOptions.Builder()
            .shadowColor(null)
            .roundedLabelColor(Color.BLACK)
            .textColor(Color.WHITE)
            .build()

        private val LIGHT_OPTIONS = CarRoadLabelOptions.Builder()
            .roundedLabelColor(Color.WHITE)
            .textColor(Color.BLACK)
            .build()

        private val EMPTY_BITMAP = createBitmap(1, 1).apply {
            eraseColor(Color.TRANSPARENT)
        }
    }
}
