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
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalOptions
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.utils.WaypointsController
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_replay_waypoints_layout.container
import kotlinx.android.synthetic.main.activity_replay_waypoints_layout.mapView
import kotlinx.android.synthetic.main.activity_replay_waypoints_layout.seekBar
import kotlinx.android.synthetic.main.activity_replay_waypoints_layout.seekBarText
import kotlinx.android.synthetic.main.activity_replay_waypoints_layout.startNavigation
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.Date

/**
 * This activity shows how to use navigate to various waypoints
 * along a [DirectionsRoute] with the Navigation SDK's
 * [ArrivalObserver] and [ArrivalController].
 *
 * The activity also shows how to the Navigation SDK's
 * route replay infrastructure.
 */
class ReplayWaypointsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mapboxMap: MapboxMap? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private val firstLocationCallback = FirstLocationCallback(this)
    private val stopsController = WaypointsController()

    private val mapboxReplayer = MapboxReplayer()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replay_waypoints_layout)
        mapView.onCreate(savedInstanceState)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions).apply {
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
            navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
                .vanishRouteLineEnabled(true)
                .build()

            // Center the map at current location. Using LocationEngineProvider because the
            // replay engine won't have your last location.
            LocationEngineProvider
                .getBestLocationEngine(this)
                .getLastLocation(firstLocationCallback)
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
                .annotationsList(
                    listOf(
                        DirectionsCriteria.ANNOTATION_SPEED,
                        DirectionsCriteria.ANNOTATION_DISTANCE
                    )
                )
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
        mapboxNavigation?.setArrivalController(arrivalController)
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
            mapboxReplayer.play()
        }

        findViewById<Button>(R.id.btn_send_user_feedback)?.let { button ->
            button.setOnClickListener {
                mapboxNavigation?.postUserFeedback(
                    FeedbackEvent.GENERAL_ISSUE,
                    "User feedback test at: ${Date().time}",
                    FeedbackEvent.UI,
                    null
                )
            }
        }
    }

    private fun setupReplayControls() {
        seekBar.max = 4
        seekBar.progress = 1
        seekBarText.text = getString(R.string.replay_playback_speed_seekbar, seekBar.progress)
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    mapboxReplayer.playbackSpeed(progress.toDouble())
                    seekBarText.text = getString(R.string.replay_playback_speed_seekbar, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )

        findViewById<Button>(R.id.navigateNextRouteLeg).setOnClickListener {
            mapboxNavigation?.navigateNextRouteLeg()
        }
    }

    private val arrivalController = object : ArrivalController {
        val arrivalOptions = ArrivalOptions.Builder()
            .arrivalInSeconds(5.0)
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
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
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

        mapboxReplayer.finish()
        mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private class FirstLocationCallback(activity: ReplayWaypointsActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let { location ->
                activityRef.get()?.let { activity ->
                    val locationEvent = ReplayRouteMapper.mapToUpdateLocation(0.0, location)
                    val locationEventList = Collections.singletonList(locationEvent)
                    activity.mapboxReplayer.pushEvents(locationEventList)
                    activity.mapboxReplayer.playFirstLocation()
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
                navigationMapboxMap?.hideRoute()
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
