package com.mapbox.navigation.examples.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.Toast
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/**
 * A utility for drawing a line on a map and using map matching to get a route.
 * When enable() is called the utility will listen for long click events on the map and for each point
 * a dot will be placed on the map with a line between representing the collective points received.
 *
 * Calling fetchRoute() will attempt to fetch a map matched route matching the points that
 * have been configured.  When a route is received draw it on the map and be sure to set the route
 * on your [MapboxNavivation] instance.
 *
 * Suggested usage:
 * Instantiate this class after the Map Style is loaded and ready.
 *
 * Call enable() to activate the long press listener which will collect the points used for creating
 * a route.
 *
 * Call disable() to deactivate the long press listener.
 *
 * When finished establishing points for the route you want to create call fetchRoute(). If a route
 * was received successfully it will be passed to the [RoutesRequestCallback] else a toast will appear
 * with some error information, also the error information will be logged.
 *
 * When a route is received call clear() this utility's line.
 *
 * Another useful function is removeLastPoint() which will remove the last point added. This could
 * be useful as a sort of "undo" if you make a mistake in where you press on the map. You may need
 * to temporarily add a button to your layout in order to make use of this function.
 *
 */

class RouteDrawingUtil(private val mapView: MapView) {

    private val touchPoints = mutableListOf<Point>()

    companion object {
        const val LINE_LAYER_SOURCE_ID = "DRAW_UTIL_LINE_LAYER_SOURCE_ID"
        const val LINE_LAYER_ID = "DRAW_UTIL_LINE_LAYER_ID"
        const val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
        const val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
        private const val LINE_COLOR = "#ffcc00"
        private const val LINE_WIDTH = 5f
        private const val LINE_OPACITY = 1f
    }

    init {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                val drawingLayerSource = style.getSourceAs<GeoJsonSource>(
                    LINE_LAYER_SOURCE_ID
                )
                if (drawingLayerSource == null) {
                    style.addSource(GeoJsonSource(LINE_LAYER_SOURCE_ID))
                }

                val lineEndSource = style.getSourceAs<GeoJsonSource>(
                    LINE_END_SOURCE_ID
                )
                if (lineEndSource == null) {
                    style.addSource(GeoJsonSource(LINE_END_SOURCE_ID))
                }

                val drawingLayer = style.getLayerAs<LineLayer>(LINE_LAYER_ID)
                if (drawingLayer == null) {
                    style.addLayer(
                        LineLayer(
                            LINE_LAYER_ID,
                            LINE_LAYER_SOURCE_ID
                        ).withProperties(
                            lineWidth(LINE_WIDTH),
                            lineJoin(LINE_JOIN_ROUND),
                            lineOpacity(LINE_OPACITY),
                            lineColor(Color.parseColor(LINE_COLOR))
                        )
                    )
                }

                val lineEndLayer = style.getLayerAs<CircleLayer>(LINE_END_LAYER_ID)
                if (lineEndLayer == null) {
                    style.addLayer(
                        CircleLayer(
                            LINE_END_LAYER_ID,
                            LINE_END_SOURCE_ID
                        ).withProperties(
                            circleRadius(5f),
                            circleOpacity(1f),
                            circleColor(Color.BLACK)
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enable() {
        mapView.getMapAsync { map ->
            map.addOnMapLongClickListener(mapLongClickListener)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun disable() {
        mapView.getMapAsync { map ->
            map.removeOnMapLongClickListener(mapLongClickListener)
        }
    }

    fun clear() {
        touchPoints.clear()
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                    ?.setGeoJson(LineString.fromLngLats(listOf()))
                style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                    ?.setGeoJson(FeatureCollection.fromFeatures(listOf()))
            }
        }
    }

    private val mapLongClickListener = MapboxMap.OnMapLongClickListener { latLng ->
        touchPoints.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                when (touchPoints.size) {
                    0 -> {
                    }
                    1 -> {
                        style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                            ?.setGeoJson(touchPoints.first())
                    }
                    else -> {
                        style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                            ?.setGeoJson(LineString.fromLngLats(touchPoints))
                        style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                            ?.setGeoJson(getFeatureCollection(touchPoints))
                    }
                }
            }
        }

        true
    }

    private fun getFeatureCollection(points: List<Point>): FeatureCollection {
        return points.map {
            Feature.fromGeometry(it)
        }.run {
            FeatureCollection.fromFeatures(this)
        }
    }

    fun removeLastPoint() {
        if (touchPoints.isNotEmpty()) {
            touchPoints.removeLast()
            mapView.getMapAsync { map ->
                map.getStyle { style ->
                    style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                        ?.setGeoJson(LineString.fromLngLats(touchPoints))
                    style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                        ?.setGeoJson(getFeatureCollection(touchPoints))
                }
            }
        }
    }

    fun fetchRoute(routeReadyCallback: RoutesRequestCallback) {
        if (touchPoints.size < 2) {
            return
        }

        val mapMatching = MapboxMapMatching.builder()
            .accessToken(Utils.getMapboxAccessToken(mapView.context))
            .coordinates(touchPoints)
            .waypointIndices(0, touchPoints.size - 1)
            .steps(true)
            .bannerInstructions(false)
            .voiceInstructions(false)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .build()

        mapMatching.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    val route = response.body()?.matchings()?.get(0)?.toDirectionRoute()
                    if (route == null) {
                        Timber.e(
                            "Failed to get a route with " +
                                "message ${response.code()} ${response.message()}"
                        )
                        Toast.makeText(
                            mapView.context,
                            "Failed to get a route with " +
                                "message ${response.code()} ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        clear()
                        enable()
                    } else {
                        routeReadyCallback.onRoutesReady(listOf(route))
                    }
                }
            }
        )
    }
}
