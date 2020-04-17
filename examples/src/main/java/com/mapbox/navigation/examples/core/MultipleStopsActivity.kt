package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.history.ReplayHistoryLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayHistoryPlayer
import com.mapbox.navigation.core.replay.history.ReplayRouteMapper
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.*
import timber.log.Timber

/**
 * To ensure proper functioning of this example make sure your Location is turned on.
 */
class MultipleStopsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mapboxMap: MapboxMap? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null
    private val firstLocationCallback = FirstLocationCallback(this)
    private val stopsController = StopsController()

    private val replayRouteMapper = ReplayRouteMapper()
    private val replayHistoryPlayer = ReplayHistoryPlayer()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.replay_engine_example_activity_layout)
        mapView.onCreate(savedInstanceState)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this,
            Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
            applicationContext,
            Utils.getMapboxAccessToken(this),
            mapboxNavigationOptions,
            locationEngine = ReplayHistoryLocationEngine(replayHistoryPlayer)
        )
        initListeners()
        mapView.getMapAsync(this)
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
            .show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap)
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }
            initLocationComponent(mapboxNavigation!!.locationEngine, style)
        }
        mapboxMap.addOnMapLongClickListener { latLng ->
            stopsController.add(latLng)

            mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
                requestRoute(originLocation)
            }
            true
        }
    }

    private fun requestRoute(originLocation: Location) {
        mapboxNavigation?.requestRoutes(
            RouteOptions.builder().applyDefaultParams()
                .accessToken(Utils.getMapboxAccessToken(applicationContext))
                .coordinates(stopsController.coordinates(originLocation))
                .alternatives(true)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build(),
            routesReqCallback
        )
    }

    private fun initLocationComponent(locationEngine: LocationEngine, loadedMapStyle: Style) {
        mapboxMap?.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap?.locationComponent.let { locationComponent ->
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationEngine(locationEngine)
                    .build()

            locationComponent?.activateLocationComponent(locationComponentActivationOptions)
            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.COMPASS
        }
        LocationEngineProvider.getBestLocationEngine(this)
            .getLastLocation(firstLocationCallback)
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                val updateLocations = replayRouteMapper.mapToUpdateLocations(0.0, routes[0])
                replayHistoryPlayer.pushEvents(updateLocations)
                replayHistoryPlayer.seekTo(updateLocations.first())
                startNavigation.visibility = View.VISIBLE
            } else {
                startNavigation.visibility = View.GONE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    @SuppressLint("MissingPermission")
    fun initListeners() {
        startNavigation.setOnClickListener {
            navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            navigationMapboxMap?.updateLocationLayerRenderMode(RenderMode.GPS)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
            replayHistoryPlayer.play(this)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationMapboxMap?.onStart()
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
        navigationMapboxMap?.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        replayHistoryPlayer.finish()
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private class FirstLocationCallback(activity: MultipleStopsActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let { location ->
                activityRef.get()?.let { activity ->
                    val locationEvent = activity.replayRouteMapper
                        .mapToUpdateLocation(0.0, location)
                    activity.replayHistoryPlayer.pushEvents(locationEvent)
                    activity.replayHistoryPlayer.playFirstLocation()
                }
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private class StopsController {
        private val stops = mutableListOf<Point>()

        fun add(latLng: LatLng) {
            stops.add(latLng.toPoint())
        }

        fun coordinates(originLocation: Location): List<Point> {
            val coordinates = mutableListOf<Point>()
            coordinates.add(originLocation.toPoint())
            coordinates.addAll(stops)
            return coordinates
        }
    }
}
