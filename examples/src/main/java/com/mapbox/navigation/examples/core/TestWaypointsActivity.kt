package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.*
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.location.ReplayRouteLocationEngine
import com.mapbox.navigation.core.trip.session.*
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.NavigationMapboxMapInstanceState
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import kotlinx.android.synthetic.main.activity_instruction_view_layout.*
import kotlinx.android.synthetic.main.test_waypoints_activity_layout.mapView
import kotlinx.android.synthetic.main.test_waypoints_activity_layout.startNavigation
import okhttp3.Cache
import timber.log.Timber
import java.io.File
import java.util.*


class TestWaypointsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val MAP_INSTANCE_STATE_KEY = "navgation_mapbox_map_instance_state"
        const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
        private const val VOICE_INSTRUCTION_CACHE = "voice-instruction-cache"
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: NavigationMapboxMapInstanceState? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var speechPlayer: NavigationSpeechPlayer


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_waypoints_activity_layout)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
                this,
                Utils.getMapboxAccessToken(this)
        )

        mapboxNavigation = MapboxNavigation(
                applicationContext,
                Utils.getMapboxAccessToken(this),
                mapboxNavigationOptions,
                locationEngine = getLocationEngine()
        ).also {
            it.registerRouteProgressObserver(routeProgressObserver)
        }

        initListeners()
        initializeSpeechPlayer()
    }

    val waypoints = listOf(
            Point.fromLngLat(-77.038294672966, 38.91854721860871),
            Point.fromLngLat(-77.03662633895874, 38.91920249133305),
            Point.fromLngLat(-77.03660779663086, 38.9182091456012),
            Point.fromLngLat(-77.03660052546692, 38.91565476415369),
            Point.fromLngLat(-77.03664779663086, 38.91398518410977),
            Point.fromLngLat(-77.03661561012268, 38.913726395687185),
            Point.fromLngLat(-77.03665852546692, 38.91345925826118),
            Point.fromLngLat(-77.03665852546692, 38.91270792885979)
    )
    val names = listOf(
            "animal", // origin
            "ocelot",
            "bear",
            "cat",
            "dog",
            "elephant",
            "frog",
            "goat" // destination
    )

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            initLocationComponent(style, mapboxMap)
            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).also {
                it.addProgressChangeListener(mapboxNavigation!!)
                mapInstanceState?.let { state ->
                    it.restoreFrom(state)
                }
            }
            gotoPredefinedLocation(mapboxMap)
        }
//        mapboxMap.addOnMapLongClickListener { latLng ->
//            //mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
//            mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(waypoints[0].latitude(), waypoints[0].longitude())))
//            mapboxNavigation?.registerBannerInstructionsObserver(bannerInstructionObserver)
//            mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
//                mapboxNavigation?.requestRoutes(
//                        RouteOptions.builder().applyDefaultParams()
//                                .accessToken(Utils.getMapboxAccessToken(applicationContext))
//                                .coordinates(waypoints.first(), waypoints.subList(1, waypoints.lastIndex), waypoints.last())
//                                .alternatives(true)
//                                .waypointNamesList(names)
//                                .profile(DirectionsCriteria.PROFILE_DRIVING)
//                                .build(),
//                        routesReqCallback
//                )
//            //}
//            true
//        }
        locationComponent = mapboxMap.locationComponent
    }

    fun gotoPredefinedLocation(mapboxMap: MapboxMap) {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(waypoints[0].latitude(), waypoints[0].longitude())))
        mapboxNavigation?.registerBannerInstructionsObserver(bannerInstructionObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                        .accessToken(Utils.getMapboxAccessToken(applicationContext))
                        .coordinates(waypoints.first(), waypoints.subList(1, waypoints.lastIndex), waypoints.last())
                        .alternatives(true)
                        .waypointNamesList(names)
                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                        .build(),
                routesReqCallback
        )
    }

    @SuppressLint("RestrictedApi")
    fun initLocationComponent(loadedMapStyle: Style, mapboxMap: MapboxMap) {
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.locationComponent.let { locationComponent ->
            val locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .build()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

            initLocationEngine()
        }
    }

    fun initLocationEngine() {
        val requestLocationUpdateRequest =
                LocationEngineRequest.Builder(BasicNavigationActivity.DEFAULT_INTERVAL_IN_MILLISECONDS)
                        .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                        .setMaxWaitTime(BasicNavigationActivity.DEFAULT_MAX_WAIT_TIME)
                        .build()

        mapboxNavigation?.locationEngine?.requestLocationUpdates(
                requestLocationUpdateRequest,
                locationListenerCallback,
                mainLooper
        )
        mapboxNavigation?.locationEngine?.getLastLocation(locationListenerCallback)
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                if (shouldSimulateRoute()) {
                    (mapboxNavigation?.locationEngine as ReplayRouteLocationEngine).assign(routes[0])
                }
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
            if (mapboxNavigation?.getRoutes()?.isNotEmpty() == true) {
                navigationMapboxMap?.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap?.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap?.startCamera(mapboxNavigation?.getRoutes()!![0])
            }
            mapboxNavigation?.startTripSession()
            startNavigation.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapboxNavigation?.registerLocationObserver(locationObserver)
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
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.unregisterBannerInstructionsObserver(bannerInstructionObserver)
        mapboxNavigation?.stopTripSession()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationMapboxMap?.saveStateWith(BasicNavigationActivity.MAP_INSTANCE_STATE_KEY, outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mapInstanceState = savedInstanceState?.getParcelable(BasicNavigationActivity.MAP_INSTANCE_STATE_KEY)
    }

    private val locationListenerCallback: LocationEngineCallback<LocationEngineResult> =
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    // todo
                }

                override fun onFailure(exception: Exception) {
                    Timber.i(exception)
                }
            }

    private fun stopLocationUpdates() {
        mapboxNavigation?.locationEngine?.removeLocationUpdates(locationListenerCallback)
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
        ) {
            if (keyPoints.isNotEmpty()) {
                locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                locationComponent?.forceLocationUpdate(enhancedLocation)
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            // do something with the route progress
            instructionView.updateDistanceWith(routeProgress)
            Timber.i("route progress: ${routeProgress.currentState()}")
        }
    }

    // Used to determine if the ReplayRouteLocationEngine should be used to simulate the routing.
    // This is used for testing purposes.
    private fun shouldSimulateRoute(): Boolean {
        return true
//        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
//                .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    // If shouldSimulateRoute is true a ReplayRouteLocationEngine will be used which is intended
    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute()) {
            ReplayRouteLocationEngine()
        } else {
            LocationEngineProvider.getBestLocationEngine(this)
        }
    }


    private val bannerInstructionObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            instructionView.updateBannerInstructionsWith(bannerInstructions)
        }
    }

    private val voiceInstructionsObserver = object : VoiceInstructionsObserver {
        override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
            speechPlayer.play(voiceInstructions)
        }
    }

    private fun initializeSpeechPlayer() {
        val cache =
                Cache(File(application.cacheDir, VOICE_INSTRUCTION_CACHE), 10 * 1024 * 1024)
        val voiceInstructionLoader =
                VoiceInstructionLoader(application, Mapbox.getAccessToken(), cache)
        val speechPlayerProvider =
                SpeechPlayerProvider(application, Locale.US.language, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

}
