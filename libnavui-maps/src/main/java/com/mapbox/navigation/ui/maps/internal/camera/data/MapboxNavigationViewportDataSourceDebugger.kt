package com.mapbox.navigation.ui.maps.internal.camera.data

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

class MapboxNavigationViewportDataSourceDebugger(
    private val context: Context,
    private val mapView: MapView
) {
    private val pointsSourceId = "mbx_viewport_data_source_points_source"
    private val pointsLayerId = "mbx_viewport_data_source_points_layer"

    private val mapboxMap = mapView.getMapboxMap()

    var enabled = false
        set(value) {
            field = value
            if (value) {
                mapView.addView(mapPaddingBorder)
                mapView.addView(userPaddingBorder)
                mapView.addView(cameraCenter)
                mapboxMap.addOnCameraChangeListener(cameraChangeListener)
            } else {
                mapView.removeView(cameraCenter)
                mapView.removeView(userPaddingBorder)
                mapView.removeView(mapPaddingBorder)
                mapboxMap.removeOnCameraChangeListener(cameraChangeListener)
                mapboxMap.getStyle()?.removeStyleLayer(pointsLayerId)
                mapboxMap.getStyle()?.removeStyleSource(pointsSourceId)
            }
            updateMapCameraCenter()
            updateMapPadding()
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
            (6 * context.resources.displayMetrics.density).toInt()
        )
        layoutParams = params
        setBackgroundColor(Color.RED)
    }

    private val cameraChangeListener = object : OnCameraChangeListener {
        override fun onCameraChanged() {
            mapView.post {
                updateMapCameraCenter()
                updateMapPadding()
            }
        }
    }

    private fun updateMapCameraCenter() {
        val center = mapboxMap.pixelForCoordinate(mapboxMap.getCameraOptions().center!!)
        cameraCenter.x = center.x.toFloat() - cameraCenter.width / 2
        cameraCenter.y = center.y.toFloat() - cameraCenter.height / 2
    }

    private fun updateMapPadding() {
        val padding = mapboxMap.getCameraOptions().padding!!

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
            NavigationCameraState.FOLLOWING -> {
                userPaddingBorder.visibility = View.VISIBLE
                followingUserPadding
            }
            NavigationCameraState.TRANSITION_TO_OVERVIEW,
            NavigationCameraState.OVERVIEW -> {
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
            NavigationCameraState.FOLLOWING -> {
                followingPoints
            }
            NavigationCameraState.TRANSITION_TO_OVERVIEW,
            NavigationCameraState.OVERVIEW -> {
                overviewPoints
            }
        }

        val lineString = LineString.fromLngLats(points)
        val style = mapboxMap.getStyle()
        if (enabled && style != null && style.fullyLoaded) {
            if (!style.styleSourceExists(pointsSourceId)) {
                val source = GeoJsonSource(
                    GeoJsonSource.Builder(pointsSourceId).geometry(lineString)
                )
                style.addSource(source)
            }

            if (!style.styleLayerExists(pointsLayerId)) {
                val layer = LineLayer(pointsLayerId, pointsSourceId).apply {
                    lineColor(Color.CYAN)
                    lineWidth(5.0)
                }
                style.addLayerAbove(layer, "road-label")
            }

            val source = style.getSource(pointsSourceId) as GeoJsonSource
            source.geometry(lineString)
        }
    }
}
