package com.mapbox.navigation.examples.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapLongClickListener
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A utility for drawing a line on a map and using map matching to get a route.
 * When enable() is called the utility will listen for long click events on the map and for each point
 * a dot will be placed on the map with a line between representing the collective points received.
 *
 * Calling fetchRoute() will attempt to fetch a map matched route matching the points that
 * have been configured.  When a route is received draw it on the map and be sure to set the route
 * on your [MapboxNavigation] instance.
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
 * was received successfully it will be passed to the [RouterCallback] else a toast will appear
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
        private const val TAG = "RouteDrawingUtil"
        const val LINE_LAYER_SOURCE_ID = "DRAW_UTIL_LINE_LAYER_SOURCE_ID"
        const val LINE_LAYER_ID = "DRAW_UTIL_LINE_LAYER_ID"
        const val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
        const val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
        private const val LINE_COLOR = "#ffcc00"
        private const val LINE_WIDTH = 5.0
        private const val LINE_OPACITY = 1.0
    }

    init {
        mapView.getMapboxMap().getStyle { style ->
            if (!style.styleSourceExists(LINE_LAYER_SOURCE_ID)) {
                geoJsonSource(LINE_LAYER_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                }.bindTo(style)
            }

            if (!style.styleSourceExists(LINE_END_SOURCE_ID)) {
                geoJsonSource(LINE_END_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(listOf()))
                }.bindTo(style)
            }

            if (!style.styleLayerExists(LINE_LAYER_ID)) {
                LineLayer(LINE_LAYER_ID, LINE_LAYER_SOURCE_ID)
                    .lineWidth(LINE_WIDTH)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(LINE_OPACITY)
                    .lineColor(Color.parseColor(LINE_COLOR))
                    .bindTo(style)
            }

            if (!style.styleLayerExists(LINE_END_LAYER_ID)) {
                CircleLayer(LINE_END_LAYER_ID, LINE_END_SOURCE_ID)
                    .circleRadius(5.0)
                    .circleOpacity(1.0)
                    .circleColor(Color.BLACK)
                    .bindTo(style)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enable() {
        mapView.getMapboxMap().addOnMapLongClickListener(mapLongClickListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun disable() {
        mapView.getMapboxMap().removeOnMapLongClickListener(mapLongClickListener)
    }

    fun clear() {
        touchPoints.clear()
        mapView.getMapboxMap().getStyle { style ->
            style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)?.featureCollection(
                FeatureCollection.fromFeatures(listOf())
            )
            style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)?.featureCollection(
                FeatureCollection.fromFeatures(listOf())
            )
        }
    }

    private val mapLongClickListener = OnMapLongClickListener { point ->
        touchPoints.add(point)
        mapView.getMapboxMap().getStyle { style ->
            when (touchPoints.size) {
                0 -> {
                }
                1 -> {
                    style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)?.feature(
                        Feature.fromGeometry(touchPoints.first())
                    )
                }
                else -> {
                    style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)?.feature(
                        Feature.fromGeometry(LineString.fromLngLats(touchPoints))
                    )
                    style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)?.featureCollection(
                        getFeatureCollection(touchPoints)
                    )
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

    fun addPoint(point: Point) {
        mapLongClickListener.onMapLongClick(point)
    }

    fun removeLastPoint() {
        if (touchPoints.isNotEmpty()) {
            touchPoints.removeLast()
            mapView.getMapboxMap().getStyle { style ->
                style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)?.feature(
                    Feature.fromGeometry(LineString.fromLngLats(touchPoints))
                )
                style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)?.featureCollection(
                    getFeatureCollection(touchPoints)
                )
            }
        }
    }

    fun fetchRoute(routeReadyCallback: RouterCallback) {
        if (touchPoints.size < 2) {
            return
        }

        val mapMatching = MapboxMapMatching.builder()
            .accessToken(Utils.getMapboxAccessToken(mapView.context))
            .coordinates(touchPoints)
            .waypointIndices(0, touchPoints.lastIndex)
            .steps(true)
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .bannerInstructions(false)
            .voiceInstructions(false)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .annotations(
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            )
            .overview("full")
            .build()

        mapMatching.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Log.e(TAG, "MapMatching request failure $t")
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    if (response.body()?.matchings()?.size ?: 0 == 0) {
                        Log.e(
                            TAG,
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
                        val route = response.body()?.matchings()?.get(0)?.toDirectionRoute()!!
                        routeReadyCallback.onRoutesReady(listOf(route), RouterOrigin.Offboard)
                    }
                }
            }
        )
    }
}
