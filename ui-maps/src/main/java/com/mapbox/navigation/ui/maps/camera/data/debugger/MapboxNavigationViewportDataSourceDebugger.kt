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
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal

/**
 * **This feature is currently experimental an subject to change.**
 *
 * Draw various info on the screen when the [NavigationCamera] operates to together with
 * the [MapboxNavigationViewportDataSource]. This info includes:
 * - Green Box, which is the padding applied by the developer for framing purposes.
 * - Black Box, which is the padding applied to the Map instance.
 * - Red Box, which is the Map's camera center.
 * - Light Blue Line, which shows the framed geometries (following and route overview).
 * - Light Blue Dots, which show the individual framed points in the points overview. Those points
 * are not a geometry, so they are not connected with a line.
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
    private val pointsOnlySourceId = "mbx_viewport_data_source_points_only_source"
    private val pointsOnlyLayerId = "mbx_viewport_data_source_points_only_layer"

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
                mapView.addView(mapWidthLabel)
                cameraChangedSubscription = mapboxMap.subscribeCameraChangedCoalesced(
                    cameraChangeCallback,
                )
            } else {
                mapView.removeView(cameraCenter)
                mapView.removeView(userPaddingBorder)
                mapView.removeView(mapPaddingBorder)
                mapView.removeView(mapWidthLabel)
                cameraChangedSubscription?.cancel()
                removePointsLayers()
            }
            val initialCameraState = mapboxMap.cameraState
            updateMapCameraCenter(initialCameraState.center)
            updateMapPadding(initialCameraState.padding)
            updateUserPadding()
            updateMapWidthLabel()
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

    /**
     * Route geometry framed by the route overview camera. Rendered as the light blue line.
     */
    internal var routePointsForOverview = listOf<Point>()
        set(value) {
            field = value
            updatePoints()
        }

    /**
     * Standalone points framed by the points overview camera. These are not a geometry, so they are
     * rendered as individual markers rather than the light blue line.
     */
    internal var pointsOnlyForOverview = listOf<Point>()
        set(value) {
            field = value
            updatePoints()
        }
    internal var cameraState = NavigationCameraStateInternal.IDLE
        set(value) {
            field = value
            updateUserPadding()
            updatePoints()
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

    private val mapWidthLabel = ScaleIndicatorView(context)

    @OptIn(MapboxExperimental::class)
    private val cameraChangeCallback = CameraChangedCoalescedCallback {
        mapView.post {
            updateMapCameraCenter(it.cameraState.center)
            updateMapPadding(it.cameraState.padding)
            updateMapWidthLabel()
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
            NavigationCameraStateInternal.IDLE -> {
                userPaddingBorder.visibility = View.GONE
                return
            }
            NavigationCameraStateInternal.TRANSITION_TO_FOLLOWING,
            NavigationCameraStateInternal.FOLLOWING,
            -> {
                userPaddingBorder.visibility = View.VISIBLE
                followingUserPadding
            }
            NavigationCameraStateInternal.TRANSITION_TO_ROUTE_OVERVIEW,
            NavigationCameraStateInternal.ROUTE_OVERVIEW,
            NavigationCameraStateInternal.TRANSITION_TO_POINTS_OVERVIEW,
            NavigationCameraStateInternal.POINTS_OVERVIEW,
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

    private fun updateMapWidthLabel() {
        if (!enabled) {
            return
        }
        mapWidthLabel.update(mapboxMap, mapView)
    }

    private fun updatePoints() {
        if (!enabled) {
            return
        }

        // Geometries (route/following) are drawn as a line, standalone points overview points are
        // drawn as individual markers - connecting unrelated points with a line would be misleading.
        val geometryPoints: List<Point>
        val standalonePoints: List<Point>
        when (cameraState) {
            NavigationCameraStateInternal.IDLE -> {
                removePointsLayers()
                return
            }
            NavigationCameraStateInternal.TRANSITION_TO_FOLLOWING,
            NavigationCameraStateInternal.FOLLOWING,
            -> {
                geometryPoints = followingPoints
                standalonePoints = emptyList()
            }
            NavigationCameraStateInternal.TRANSITION_TO_ROUTE_OVERVIEW,
            NavigationCameraStateInternal.ROUTE_OVERVIEW,
            -> {
                geometryPoints = routePointsForOverview
                standalonePoints = emptyList()
            }
            NavigationCameraStateInternal.TRANSITION_TO_POINTS_OVERVIEW,
            NavigationCameraStateInternal.POINTS_OVERVIEW,
            -> {
                geometryPoints = emptyList()
                standalonePoints = pointsOnlyForOverview
            }
        }

        val style = mapboxMap.getStyle() ?: return

        val lineFeatures = if (geometryPoints.size > 1) {
            FeatureCollection.fromFeature(
                Feature.fromGeometry(LineString.fromLngLats(geometryPoints)),
            )
        } else {
            FeatureCollection.fromFeatures(emptyList())
        }
        updateSource(style, pointsSourceId, lineFeatures) {
            LineLayer(pointsLayerId, pointsSourceId).apply {
                lineColor(Color.CYAN)
                lineWidth(5.0)
            }
        }

        val circleFeatures = FeatureCollection.fromFeatures(
            standalonePoints.map { Feature.fromGeometry(it) },
        )
        updateSource(style, pointsOnlySourceId, circleFeatures) {
            CircleLayer(pointsOnlyLayerId, pointsOnlySourceId).apply {
                circleColor(Color.CYAN)
                circleRadius(6.0)
                circleStrokeColor(Color.BLACK)
                circleStrokeWidth(1.0)
            }
        }
    }

    private fun updateSource(
        style: Style,
        sourceId: String,
        featureCollection: FeatureCollection,
        layerProvider: () -> Layer,
    ) {
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(geoJsonSource(sourceId) {}.featureCollection(featureCollection))
        }

        val layer = layerProvider()
        if (!style.styleLayerExists(layer.layerId)) {
            if (layerAbove != null && style.styleLayerExists(layerAbove)) {
                style.addLayerAbove(layer, layerAbove)
            } else {
                style.addLayer(layer)
            }
        }

        (style.getSource(sourceId) as GeoJsonSource).featureCollection(featureCollection)
    }

    private fun removePointsLayers() {
        mapboxMap.getStyle()?.apply {
            removeStyleLayer(pointsLayerId)
            removeStyleSource(pointsSourceId)
            removeStyleLayer(pointsOnlyLayerId)
            removeStyleSource(pointsOnlySourceId)
        }
    }
}
