package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import java.util.Date
import java.util.Locale
import kotlinx.android.synthetic.main.content_simple_mapbox_navigation.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Cache
import timber.log.Timber

class DebugMapboxNavigationKt : AppCompatActivity(), OnMapReadyCallback,
        VoiceInstructionsObserver {

    companion object {
        private const val VOICE_INSTRUCTION_CACHE = "voice-instruction-cache"
    }
    private val locationEngineCallback = MyLocationEngineCallback(this)
    private val restartSessionEventChannel = Channel<RestartTripSessionAction>(1)

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null
    private lateinit var originalRoute: DirectionsRoute

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var localLocationEngine: LocationEngine
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var speechPlayer: NavigationSpeechPlayer
    private val replayRouteLocationEngine = ReplayRouteLocationEngine()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_mapbox_navigation)
        findViewById<Button>(R.id.btn_send_user_feedback)?.let { button ->
            button.setOnClickListener {
                MapboxNavigation.postUserFeedback(
                        FeedbackEvent.GENERAL_ISSUE,
                        "User feedback test at: ${Date().time}",
                        FeedbackEvent.UI,
                        null
                )
            }
        }
        findViewById<Button>(R.id.btn_add_original_route)?.let { button ->
            button.setOnClickListener {
                if (::originalRoute.isInitialized) {
                    val routes = mapboxNavigation.getRoutes()
                    if (routes.isNotEmpty()) {
                        mapboxNavigation.setRoutes(mapboxNavigation.getRoutes().toMutableList().apply {
                            removeAt(0)
                            add(0, originalRoute)
                        })
                    } else {
                        mapboxNavigation.setRoutes(listOf(originalRoute))
                    }
                }
            }
        }
        findViewById<Button>(R.id.btn_clear_routes)?.let { button ->
            button.setOnClickListener {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
        findViewById<Button>(R.id.btn_clear_points)?.let { button ->
            button.setOnClickListener {
                symbolManager?.deleteAll()
            }
        }
        initViews()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        val options =
                MapboxNavigation.defaultNavigationOptions(this, Utils.getMapboxAccessToken(this))

        val tilesUri = URI("https://api-routing-tiles-staging.tilestream.net")
        val tilesVersion = "2020_02_02-03_00_00"

        val endpoint = options.onboardRouterConfig?.endpoint?.toBuilder()
                ?.host(tilesUri.toString())
                ?.version(tilesVersion)
                ?.build()

        val onboardRouterConfig = options.onboardRouterConfig?.toBuilder()
                ?.tilePath(
                        File(
                                filesDir,
                                "Offline/${tilesUri.host}/$tilesVersion"
                        ).absolutePath
                )
                ?.endpoint(endpoint)
                ?.build()

        val newOptions =
                options.toBuilder()
                        .onboardRouterConfig(onboardRouterConfig)
                        .navigatorPredictionMillis(1000L)
                        .build()

        mapboxNavigation = getMapboxNavigation(newOptions)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.addOnMapLongClickListener { click ->
            locationComponent?.lastKnownLocation?.let { location ->
                mapboxNavigation.requestRoutes(
                        RouteOptions.builder().applyDefaultParams()
                                .accessToken(Utils.getMapboxAccessToken(applicationContext))
                                .coordinates(location.toPoint(), null, click.toPoint())
                                .alternatives(true)
                                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                                .build(),
                        routesReqCallback
                )

                symbolManager?.deleteAll()
                symbolManager?.create(
                        SymbolOptions()
                                .withIconImage("marker")
                                .withGeometry(click.toPoint())
                )
            }
            false
        }

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                        LocationComponentActivationOptions.builder(this@DebugMapboxNavigationKt, style)
                                .useDefaultLocationEngine(false)
                                .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }
            symbolManager = SymbolManager(mapView, mapboxMap, style)
            style.addImage("marker", IconFactory.getInstance(this).defaultMarker().bitmap)
            style.addImage("raw", ContextCompat.getDrawable(this, R.drawable.ic_circle_red)!!)
            style.addImage("enhanced", ContextCompat.getDrawable(this, R.drawable.ic_circle_blue)!!)

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap)
            navigationMapboxMap.setCamera(DynamicCamera(mapboxMap))
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap.setOnRouteSelectionChangeListener { route ->
                mapboxNavigation.setRoutes(mapboxNavigation.getRoutes().toMutableList().apply {
                    remove(route)
                    add(0, route)
                })
            }
        }
        Snackbar.make(findViewById(R.id.container), R.string.msg_long_press_map_to_place_waypoint,
                LENGTH_SHORT).show()
        initializeSpeechPlayer()
    }

    private fun initializeSpeechPlayer() {
        val cache = Cache(File(application.cacheDir, VOICE_INSTRUCTION_CACHE), 10 * 1024 * 1024)
        val voiceInstructionLoader =
                VoiceInstructionLoader(application, Mapbox.getAccessToken(), cache)
        val speechPlayerProvider =
                SpeechPlayerProvider(application, Locale.US.language, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

    @SuppressLint("MissingPermission")
    private fun initViews() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.registerVoiceInstructionsObserver(this)
            mapboxNavigation.startTripSession()
            val routes = mapboxNavigation.getRoutes()
            if (routes.isNotEmpty()) {
                initDynamicCamera(routes[0])
            }
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            symbolManager?.create(
                    SymbolOptions()
                            .withIconImage("raw")
                            .withGeometry(rawLocation.toPoint())
            )
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            symbolManager?.create(
                    SymbolOptions()
                            .withIconImage("enhanced")
                            .withGeometry(enhancedLocation.toPoint())
            )
            if (keyPoints.isNotEmpty()) {
                locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                locationComponent?.forceLocationUpdate(enhancedLocation)
            }
            Timber.d("enhanced location %s", enhancedLocation)
            Timber.d("enhanced keyPoints %s", keyPoints)
        }
    }

    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(1000L)
                .setFastestInterval(500L)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()
        try {
            localLocationEngine.requestLocationUpdates(
                    request,
                    locationEngineCallback,
                    Looper.getMainLooper()
            )
            localLocationEngine.getLastLocation(locationEngineCallback)
        } catch (exception: SecurityException) {
            Timber.e(exception)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            Timber.d("route progress %s", routeProgress.toString())
            if (routeProgress.route() != null) {
                navigationMapboxMap.onNewRouteProgress(routeProgress)
            }
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapboxMap.drawRoutes(routes)
            if (routes.isEmpty()) {
                Toast.makeText(this@DebugMapboxNavigationKt, "Empty routes", Toast.LENGTH_SHORT)
                        .show()
            } else {
                if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
                    initDynamicCamera(routes[0])
                }
            }
            Timber.d("route changed %s", routes.toString())
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            originalRoute = routes[0]
            Timber.d("route request success %s", routes.toString())
            replayRouteLocationEngine.assign(originalRoute)
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            symbolManager?.deleteAll()
            Timber.d("route request canceled")
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                    startNavigation.visibility = GONE
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                    startNavigation.visibility = VISIBLE
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private fun initDynamicCamera(route: DirectionsRoute) {
        navigationMapboxMap.startCamera(route)
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()

        restartSessionEventChannel.poll()?.also {
            mapboxNavigation.registerVoiceInstructionsObserver(this)
            mapboxNavigation.startTripSession()
        }

        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.detachFasterRouteObserver()
        stopLocationUpdates()

        if (mapboxNavigation.getRoutes().isEmpty() && mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
            // use this to kill the service and hide the notification when going into the background in the Free Drive state,
            // but also ensure to restart Free Drive when coming back from background by using the channel
            mapboxNavigation.unregisterVoiceInstructionsObserver(this)
            mapboxNavigation.stopTripSession()
            restartSessionEventChannel.offer(RestartTripSessionAction)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        mapboxNavigation.unregisterVoiceInstructionsObserver(this)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()

        restartSessionEventChannel.cancel()

        speechPlayer.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
        speechPlayer.play(voiceInstructions)
    }

    private class MyLocationEngineCallback(activity: DebugMapboxNavigationKt) :
            LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                activityRef.get()?.locationComponent?.forceLocationUpdate(it)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private object RestartTripSessionAction

    private fun getMapboxNavigation(options: NavigationOptions): MapboxNavigation {
        return if (shouldSimulateRoute()) {
            return MapboxNavigation(
                    applicationContext,
                    Utils.getMapboxAccessToken(this),
                    navigationOptions = options,
                    locationEngine = replayRouteLocationEngine
            )
        } else {
            MapboxNavigation(
                    applicationContext,
                    Utils.getMapboxAccessToken(this),
                    navigationOptions = options
            )
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
                .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        if (::navigationMapboxMap.isInitialized) {
            navigationMapboxMap.apply {
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
}
