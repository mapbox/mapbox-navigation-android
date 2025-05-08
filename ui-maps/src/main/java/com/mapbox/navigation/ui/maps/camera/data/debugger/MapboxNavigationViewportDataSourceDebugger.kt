package com.mapbox.navigation.ui.maps.camera.data.debugger

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraChangedCoalescedCallback
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

/**
 * **This feature is currently experimental an subject to change.**
 *
 * Draw various info on the screen when the [NavigationCamera] operates to together with
 * the [MapboxNavigationViewportDataSource]. This info includes:
 * - Green Box, which is the padding applied by the developer for framing purposes.
 * - Black Box, which is the padding applied to the Map instance.
 * - Red Box, which is the Map's camera center.
 * - Light Blue Line, which shows the framed geometries.
 *
 * ### Example
 * Make sure to also provide the same debugger instance to [NavigationCamera.debugger] and
 * [MapboxNavigationViewportDataSource.debugger].
 *
 * ```kotlin
 * val debugger = MapboxNavigationViewportDataSourceDebugger(
 *     context,
 *     mapView
 * ).apply {
 *     enabled = true
 * }
 * viewportDataSource = MapboxNavigationViewportDataSource(
 *     mapView.getMapboxMap()
 * )
 * viewportDataSource.debugger = debugger
 * navigationCamera = NavigationCamera(
 *     mapView.getMapboxMap(),
 *     mapView.camera,
 *     viewportDataSource
 * )
 * navigationCamera.debugger = debugger
 * ```
 *
 * @param layerAbove layer in the current map style above which the debug layer with framed geometries should be placed
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxNavigationViewportDataSourceDebugger @JvmOverloads constructor(
    private val context: Context,
    private val mapView: MapView,
    private val layerAbove: String? = null,
) {
    private val pointsSourceId = "mbx_viewport_data_source_points_source"
    private val pointsLayerId = "mbx_viewport_data_source_points_layer"

    private val mapboxMap = mapView.getMapboxMap()

    /**
     * Use to show/hide the debug info.
     */
    var enabled = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                mapView.addView(mapPaddingBorder)
                mapView.addView(userPaddingBorder)
                mapView.addView(cameraCenter)
                cameraChangedSubscription = mapboxMap.subscribeCameraChangedCoalesced(
                    cameraChangeCallback,
                )
            } else {
                mapView.removeView(cameraCenter)
                mapView.removeView(userPaddingBorder)
                mapView.removeView(mapPaddingBorder)
                cameraChangedSubscription?.cancel()
                mapboxMap.getStyle()?.removeStyleLayer(pointsLayerId)
                mapboxMap.getStyle()?.removeStyleSource(pointsSourceId)
            }
            val initialCameraState = mapboxMap.cameraState
            updateMapCameraCenter(initialCameraState.center)
            updateMapPadding(initialCameraState.padding)
            updateUserPadding()
            updatePoints()
        }

    internal var followingUserPadding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        set(value) {
            field = value
            updateUserPadding()
        }
    internal var overviewUserPadding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        set(value) {
            field = value
            updateUserPadding()
        }
    internal var followingPoints = listOf<Point>()
        set(value) {
            field = value
            updatePoints()
        }
    internal var overviewPoints = listOf<Point>()
        set(value) {
            field = value
            updatePoints()
        }
    internal var cameraState = NavigationCameraState.IDLE
        set(value) {
            field = value
            updateUserPadding()
        }

    private val mapPaddingBorder = View(context).apply {
        val params = FrameLayout.LayoutParams(mapView.width, mapView.height)
        layoutParams = params
        background = ContextCompat.getDrawable(context, R.drawable.viewport_debugger_border_black)
    }
    private val userPaddingBorder = View(context).apply {
        val params = FrameLayout.LayoutParams(mapView.width, mapView.height)
        layoutParams = params
        background = ContextCompat.getDrawable(context, R.drawable.viewport_debugger_border_green)
    }
    private val cameraCenter = View(context).apply {
        val params = FrameLayout.LayoutParams(
            (6 * context.resources.displayMetrics.density).toInt(),
            (6 * context.resources.displayMetrics.density).toInt(),
        )
        layoutParams = params
        setBackgroundColor(Color.RED)
    }

    @OptIn(MapboxExperimental::class)
    private val cameraChangeCallback = CameraChangedCoalescedCallback {
        mapView.post {
            updateMapCameraCenter(it.cameraState.center)
            updateMapPadding(it.cameraState.padding)
        }
    }
    private var cameraChangedSubscription: Cancelable? = null

    private fun updateMapCameraCenter(cameraStateCenter: Point) {
        val center = mapboxMap.pixelForCoordinate(cameraStateCenter)
        cameraCenter.x = center.x.toFloat() - cameraCenter.width / 2
        cameraCenter.y = center.y.toFloat() - cameraCenter.height / 2
    }

    private fun updateMapPadding(padding: EdgeInsets) {
        val width = (mapView.width - padding.left - padding.right).toInt()
        val height = (mapView.height - padding.top - padding.bottom).toInt()
        val params = mapPaddingBorder.layoutParams

        if (width == 0) {
            params.width = (10 * context.resources.displayMetrics.density).toInt()
            mapPaddingBorder.x = padding.left.toFloat() - params.width / 2
        } else {
            params.width = width
            mapPaddingBorder.x = padding.left.toFloat()
        }

        if (height == 0) {
            params.height = (10 * context.resources.displayMetrics.density).toInt()
            mapPaddingBorder.y = padding.top.toFloat() - params.height / 2
        } else {
            params.height = height
            mapPaddingBorder.y = padding.top.toFloat()
        }

        mapPaddingBorder.layoutParams = params
    }

    private fun updateUserPadding() {
        if (!enabled) {
            return
        }

        val padding = when (cameraState) {
            NavigationCameraState.IDLE -> {
                userPaddingBorder.visibility = View.GONE
                return
            }
            NavigationCameraState.TRANSITION_TO_FOLLOWING,
            NavigationCameraState.FOLLOWING,
            -> {
                userPaddingBorder.visibility = View.VISIBLE
                followingUserPadding
            }
            NavigationCameraState.TRANSITION_TO_OVERVIEW,
            NavigationCameraState.OVERVIEW,
            -> {
                userPaddingBorder.visibility = View.VISIBLE
                overviewUserPadding
            }
        }

        val params = userPaddingBorder.layoutParams
        params.width = (mapView.width - padding.left - padding.right).toInt()
        params.height = (mapView.height - padding.top - padding.bottom).toInt()
        userPaddingBorder.layoutParams = params
        userPaddingBorder.x = padding.left.toFloat()
        userPaddingBorder.y = padding.top.toFloat()
    }

    private fun updatePoints() {
        if (!enabled) {
            return
        }

        val points = when (cameraState) {
            NavigationCameraState.IDLE -> {
                mapboxMap.getStyle()?.removeStyleLayer(pointsLayerId)
                mapboxMap.getStyle()?.removeStyleSource(pointsSourceId)
                return
            }
            NavigationCameraState.TRANSITION_TO_FOLLOWING,
            NavigationCameraState.FOLLOWING,
            -> {
                followingPoints
            }
            NavigationCameraState.TRANSITION_TO_OVERVIEW,
            NavigationCameraState.OVERVIEW,
            -> {
                overviewPoints
            }
        }

        val featureCollection = if (points.size > 1) {
            FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points)))
        } else {
            FeatureCollection.fromFeatures(emptyList())
        }

        val style = mapboxMap.getStyle()
        if (enabled && style != null) {
            if (!style.styleSourceExists(pointsSourceId)) {
                val source = geoJsonSource(pointsSourceId) {}.featureCollection(featureCollection)
                style.addSource(source)
            }

            if (!style.styleLayerExists(pointsLayerId)) {
                val layer = LineLayer(pointsLayerId, pointsSourceId).apply {
                    lineColor(Color.CYAN)
                    lineWidth(5.0)
                }
                if (layerAbove != null && style.styleLayerExists(layerAbove)) {
                    style.addLayerAbove(layer, layerAbove)
                } else {
                    style.addLayer(layer)
                }
            }

            val source = style.getSource(pointsSourceId) as GeoJsonSource
            source.featureCollection(featureCollection)
        }
    }
}
