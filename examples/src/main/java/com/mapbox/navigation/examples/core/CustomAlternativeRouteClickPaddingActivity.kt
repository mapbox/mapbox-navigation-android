package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils.dpToPx
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Projection
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_alternative_route_click_padding.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * This activity shows how to visualize and adjust what is usually an invisible RectF
 * query box. This Nav SDK builds this box around the map click location. If the
 * MapRouteClickListener determines that any route lines run through
 * this invisible box, this is used to figure out which route was selected and
 * for potentially firing the OnRouteSelectionChangeListener.
 */
open class CustomAlternativeRouteClickPaddingActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    MapboxMap.OnMapClickListener {

    companion object {
        const val CLICK_BOX_SOURCE_ID = "CLICK_BOX_SOURCE_ID"
        const val CLICK_BOX_LAYER_ID = "CLICK_BOX_LAYER_ID"
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: Bundle? = null
    private var mapClickPadding: Float = 30f
    private var lastMapClickLatLng: LatLng? = null
    private var clickToShowQuerySnackbarHasBeenShown = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alternative_route_click_padding)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(LocationEngineProvider.getBestLocationEngine(this))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.addOnMapClickListener(this)
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

            navigationMapboxMap = NavigationMapboxMap.Builder(
                mapView,
                mapboxMap,
                this
            ).withRouteClickPadding(dpToPx(mapClickPadding)).build()

            initPaddingPolygonSourceAndLayer()

            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreStateFrom(state)
            }

            mapboxNavigation
                ?.navigationOptions
                ?.locationEngine
                ?.getLastLocation(locationListenerCallback)
            Snackbar
                .make(
                    container,
                    R.string.msg_long_press_map_to_place_waypoint,
                    LENGTH_SHORT
                )
                .show()
        }
        mapboxMap.addOnMapLongClickListener { latLng ->
            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                        .alternatives(true)
                        .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                        .build(),
                    routesReqCallback
                )
            }
            true
        }
    }

    override fun onMapClick(mapClickLatLng: LatLng): Boolean {
        navigationMapboxMap?.retrieveMap()?.let { mapboxMap ->
            mapboxMap.getStyle { style ->
                lastMapClickLatLng = mapClickLatLng
                style.getSourceAs<GeoJsonSource>(CLICK_BOX_SOURCE_ID)?.let { geoJsonSource ->
                    adjustPolygonFillLayerArea(mapClickLatLng)
                }
            }
        }
        return false
    }

    private fun adjustPolygonFillLayerArea(mapClickLatLng: LatLng) {
        navigationMapboxMap?.retrieveMap()?.let { mapboxMap ->
            mapboxMap.getStyle { style ->
                style.getSourceAs<GeoJsonSource>(CLICK_BOX_SOURCE_ID)?.let { geoJsonSource ->
                    val devicePadding = dpToPx(mapClickPadding)
                    val mapProjection: Projection = mapboxMap.projection
                    val mapClickPointF = mapProjection.toScreenLocation(mapClickLatLng)
                    val leftFloat = (mapClickPointF.x - devicePadding)
                    val rightFloat = (mapClickPointF.x + devicePadding)
                    val topFloat = (mapClickPointF.y - devicePadding)
                    val bottomFloat = (mapClickPointF.y + devicePadding)

                    val listOfPointLists: MutableList<List<Point>> = ArrayList()
                    val pointList: MutableList<Point> = ArrayList()

                    val upperLeftLatLng: LatLng = mapProjection.fromScreenLocation(
                        PointF(leftFloat, topFloat)
                    )
                    val upperRightLatLng: LatLng = mapProjection.fromScreenLocation(
                        PointF(rightFloat, topFloat)
                    )
                    val lowerLeftLatLng: LatLng = mapProjection.fromScreenLocation(
                        PointF(leftFloat, bottomFloat)
                    )
                    val lowerRightLatLng: LatLng = mapProjection.fromScreenLocation(
                        PointF(rightFloat, bottomFloat)
                    )

                    pointList.apply {
                        add(Point.fromLngLat(upperLeftLatLng.longitude, upperLeftLatLng.latitude))
                        add(Point.fromLngLat(lowerLeftLatLng.longitude, lowerLeftLatLng.latitude))
                        add(Point.fromLngLat(lowerRightLatLng.longitude, lowerRightLatLng.latitude))
                        add(Point.fromLngLat(upperRightLatLng.longitude, upperRightLatLng.latitude))
                        add(Point.fromLngLat(upperLeftLatLng.longitude, upperLeftLatLng.latitude))
                        listOfPointLists.add(this)
                        geoJsonSource.setGeoJson(Polygon.fromLngLats(listOfPointLists))
                    }
                }
            }
        }
    }

    /**
     * Add a source and layer to the [Style] so that the click box [Polygon]
     * can be displayed to visualize the querying area.
     */
    private fun initPaddingPolygonSourceAndLayer() {
        navigationMapboxMap?.retrieveMap()?.getStyle {
            it.addSource(GeoJsonSource(CLICK_BOX_SOURCE_ID))
            it.addLayer(
                FillLayer(CLICK_BOX_LAYER_ID, CLICK_BOX_SOURCE_ID)
                    .withProperties(
                        PropertyFactory.fillColor(Color.RED),
                        PropertyFactory.fillOpacity(.4f)
                    )
            )
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoutes(routes)
            }
            if (clickToShowQuerySnackbarHasBeenShown) {
                Snackbar
                    .make(
                        container,
                        R.string.alternative_route_click_instruction,
                        LENGTH_SHORT
                    )
                    .show()
                clickToShowQuerySnackbarHasBeenShown = true
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: CustomAlternativeRouteClickPaddingActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.navigationMapboxMap?.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.i(exception)
        }
    }
}
