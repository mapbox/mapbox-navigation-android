package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.history.ReplayHistoryLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayHistoryPlayer
import com.mapbox.navigation.core.replay.route2.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route2.ReplayRouteMapper
import com.mapbox.navigation.core.stops.ArrivalController
import com.mapbox.navigation.core.stops.ArrivalObserver
import com.mapbox.navigation.core.stops.ArrivalOptions
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import java.lang.ref.WeakReference
import java.util.Collections
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.container
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.mapView
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.seekBar
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.seekBarText
import kotlinx.android.synthetic.main.replay_engine_example_activity_layout.startNavigation
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

    private val replayHistoryPlayer = ReplayHistoryPlayer(MapboxLogger)
    private val replayProgressObserver = ReplayProgressObserver(replayHistoryPlayer)

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
        ).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
        }

        initListeners()
        mapView.getMapAsync(this)
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
            .show()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, true)
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreFrom(state)
            }

            // Center the map at current location. Using LocationEngineProvider because the
            // replay engine won't have your last location.
            LocationEngineProvider.getBestLocationEngine(this).getLastLocation(firstLocationCallback)
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
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .annotationsList(listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_DISTANCE))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build(),
            routesReqCallback
        )
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
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
        mapboxNavigation?.attachArrivalController(arrivalController)
        mapboxNavigation?.registerArrivalObserver(arrivalObserver)
        setupReplayControls()
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation!!)
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
            replayHistoryPlayer.play(this)
        }
    }

    private fun setupReplayControls() {
        seekBar.max = 4
        seekBar.progress = 1
        seekBarText.text = getString(R.string.replay_history_player_playback_seekbar, seekBar.progress)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                replayHistoryPlayer.playbackSpeed(progress.toDouble())
                seekBarText.text = getString(R.string.replay_history_player_playback_seekbar, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })

        findViewById<Button>(R.id.navigateNextRouteLeg).setOnClickListener {
            mapboxNavigation?.navigateNextRouteLeg()
        }
    }

    private val arrivalController = object : ArrivalController {
        val arrivalOptions = ArrivalOptions.Builder()
            .arriveInSeconds(5.0)
            .build()
        override fun arrivalOptions(): ArrivalOptions = arrivalOptions

        override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
            // This example shows you can use both time and distance.
            // Move to the next step when the distance is small
            findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.VISIBLE
            return false
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onStopArrival(routeLegProgress: RouteLegProgress) {
            findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
        }

        override fun onRouteArrival(routeProgress: RouteProgress) {
            findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
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
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
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
                    val locationEvent = ReplayRouteMapper.mapToUpdateLocation(0.0, location)
                    val locationEventList = Collections.singletonList(locationEvent)
                    activity.replayHistoryPlayer.pushEvents(locationEventList)
                    activity.replayHistoryPlayer.playFirstLocation()
                    activity.navigationMapboxMap?.updateLocation(result.lastLocation)
                }
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            if (tripSessionState == TripSessionState.STOPPED) {
                stopsController.clear()
                navigationMapboxMap?.removeRoute()
                updateCameraOnNavigationStateChange(false)
            }
        }
    }

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }
}

private class StopsController {
    private val stops = mutableListOf<Point>()

    fun add(latLng: LatLng) {
        stops.add(latLng.toPoint())
    }

    fun clear() {
        stops.clear()
    }

    fun coordinates(originLocation: Location): List<Point> {
        val coordinates = mutableListOf<Point>()
        coordinates.add(originLocation.toPoint())
        coordinates.addAll(stops)
        return coordinates
    }
}
