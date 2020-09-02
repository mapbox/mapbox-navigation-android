package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
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
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.history.HistoryRecorder
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.Utils.PRIMARY_ROUTE_BUNDLE_KEY
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import kotlinx.android.synthetic.main.bottom_sheet_faster_route.*
import kotlinx.android.synthetic.main.content_simple_mapbox_navigation.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Cache
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.Date
import java.util.Locale

/**
 * This activity shows how to set up a basic turn-by-turn
 * navigation experience with many of the the Navigation SDK's
 * [MapboxNavigation] class' capabilities.
 */
class SimpleMapboxNavigationKt :
    AppCompatActivity(),
    OnMapReadyCallback,
    VoiceInstructionsObserver {

    private val VOICE_INSTRUCTION_CACHE =
        "voice-instruction-cache"
    private val startTimeInMillis = 5000L
    private val countdownInterval = 10L
    private val maxProgress = startTimeInMillis / countdownInterval
    private val locationEngineCallback = MyLocationEngineCallback(this)
    private val restartSessionEventChannel = Channel<RestartTripSessionAction>(1)

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null
    private var fasterRoutes: List<DirectionsRoute> = emptyList()
    private var originalRoute: DirectionsRoute? = null

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var localLocationEngine: LocationEngine
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var speechPlayer: NavigationSpeechPlayer
    private val mapboxReplayer = MapboxReplayer()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_mapbox_navigation)
        findViewById<Button>(R.id.btn_send_user_feedback)?.let { button ->
            button.setOnClickListener {
                mapboxNavigation.postUserFeedback(
                    FeedbackEvent.GENERAL_ISSUE,
                    "User feedback test at: ${Date().time}",
                    FeedbackEvent.UI,
                    null
                )
            }
        }
        findViewById<Button>(R.id.btn_add_original_route)?.let { button ->
            button.setOnClickListener {
                originalRoute?.let {
                    val routes = mapboxNavigation.getRoutes()
                    when (routes.isNotEmpty()) {
                        true -> mapboxNavigation.setRoutes(
                            mapboxNavigation.getRoutes().toMutableList().apply {
                                removeAt(0)
                                add(0, it)
                            }
                        )
                        false -> mapboxNavigation.setRoutes(listOf(it))
                    }
                }
            }
        }
        findViewById<Button>(R.id.btn_clear_routes)?.let { button ->
            button.setOnClickListener {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        mapboxNavigation = getMapboxNavigation(mapboxNavigationOptions)
        if (shouldRecordHistory()) {
            mapboxNavigation.registerTripSessionStateObserver(HistoryRecorder(mapboxNavigation))
        }
        initViews()
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
                    LocationComponentActivationOptions.builder(this@SimpleMapboxNavigationKt, style)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }
            symbolManager = SymbolManager(mapView, mapboxMap, style)
            style.addImage("marker", IconFactory.getInstance(this).defaultMarker().bitmap)

            navigationMapboxMap = NavigationMapboxMap(
                mapView,
                mapboxMap,
                this,
                true
            )
            navigationMapboxMap.setCamera(DynamicCamera(mapboxMap))
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap.setOnRouteSelectionChangeListener { route ->
                mapboxNavigation.setRoutes(
                    mapboxNavigation.getRoutes().toMutableList().apply {
                        remove(route)
                        add(0, route)
                    }
                )
            }
            initNavigationButton()

            when (originalRoute) {
                null -> {
                    if (shouldSimulateRoute()) {
                        mapboxNavigation
                            .registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                        mapboxReplayer.pushRealLocation(this, 0.0)
                        mapboxReplayer.play()
                    }
                    Snackbar
                        .make(
                            container,
                            R.string.msg_long_press_map_to_place_waypoint,
                            LENGTH_SHORT
                        )
                        .show()
                }
                else -> restoreNavigation()
            }
        }
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

    private fun initViews() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFasterRoute)
        bottomSheetBehavior.peekHeight = 0
        fasterRouteAcceptProgress.max = maxProgress.toInt()
        dismissLayout.setOnClickListener {
            fasterRouteSelectionTimer.onFinish()
        }
        acceptLayout.setOnClickListener { _ ->
            fasterRoutes.takeIf { it.isNotEmpty() }?.let { newRoutes ->
                mapboxNavigation.setRoutes(newRoutes)
                fasterRouteSelectionTimer.onFinish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigationButton() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.registerVoiceInstructionsObserver(this)
            mapboxNavigation.startTripSession()
            val routes = mapboxNavigation.getRoutes()
            if (routes.isNotEmpty()) {
                initDynamicCamera(routes[0])
            }
            navigationMapboxMap.showAlternativeRoutes(false)
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
            if (originalRoute == null) {
                localLocationEngine.getLastLocation(locationEngineCallback)
            }
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
            navigationMapboxMap.onNewRouteProgress(routeProgress)
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            navigationMapboxMap.drawRoutes(routes)
            if (routes.isEmpty()) {
                Toast.makeText(this@SimpleMapboxNavigationKt, "Empty routes", Toast.LENGTH_SHORT)
                    .show()
            } else {
                if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
                    initDynamicCamera(routes[0])
                }
            }
            Timber.d("route changed %s", routes.toString())
        }
    }

    private val fasterRouteSelectionTimer: CountDownTimer =
        object : CountDownTimer(startTimeInMillis, countdownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("FASTER_ROUTE: millisUntilFinished $millisUntilFinished")
                fasterRouteAcceptProgress.progress =
                    (maxProgress - millisUntilFinished / countdownInterval).toInt()
            }

            override fun onFinish() {
                Timber.d("FASTER_ROUTE: finished")
                this@SimpleMapboxNavigationKt.fasterRoutes = emptyList()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

    private val fasterRouteObserver = object : FasterRouteObserver {
        override fun onFasterRoute(
            currentRoute: DirectionsRoute,
            alternatives: List<DirectionsRoute>,
            isAlternativeFaster: Boolean
        ) {
            if (isAlternativeFaster) {
                this@SimpleMapboxNavigationKt.fasterRoutes = alternatives
                fasterRouteSelectionTimer.start()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            originalRoute = routes[0]
            navigationMapboxMap.drawRoutes(routes)
            Timber.d("route request success %s", routes.toString())
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

    private fun updateCameraOnNavigationStateChange(
        navigationStarted: Boolean
    ) {
        if (::navigationMapboxMap.isInitialized) {
            navigationMapboxMap.apply {
                if (navigationStarted) {
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                    updateLocationLayerRenderMode(RenderMode.GPS)
                } else {
                    symbolManager?.deleteAll()
                    updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                    updateLocationLayerRenderMode(RenderMode.COMPASS)
                }
            }
        }
    }

    private fun initDynamicCamera(route: DirectionsRoute) {
        if (::navigationMapboxMap.isInitialized) {
            navigationMapboxMap.startCamera(route)
        }
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

        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.attachFasterRouteObserver(fasterRouteObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.detachFasterRouteObserver()
        stopLocationUpdates()

        if (mapboxNavigation.getRoutes()
            .isEmpty() && mapboxNavigation.getTripSessionState() == TripSessionState.STARTED
        ) {
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

        mapboxReplayer.finish()
        mapboxNavigation.unregisterVoiceInstructionsObserver(this)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()

        restartSessionEventChannel.cancel()

        speechPlayer.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)

        // This is not the most efficient way to preserve the route on a device rotation.
        // This is here to demonstrate that this event needs to be handled in order to
        // redraw the route line after a rotation.
        originalRoute?.let {
            outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        originalRoute = Utils.getRouteFromBundle(savedInstanceState)
    }

    override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
        speechPlayer.play(voiceInstructions)
    }

    private class MyLocationEngineCallback(activity: SimpleMapboxNavigationKt) :
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

    private fun getMapboxNavigation(optionsBuilder: NavigationOptions.Builder): MapboxNavigation {
        return if (shouldSimulateRoute()) {
            optionsBuilder.locationEngine(ReplayLocationEngine(mapboxReplayer))
            MapboxNavigation(optionsBuilder.build()).apply {
                registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                if (originalRoute == null) {
                    mapboxReplayer.pushRealLocation(applicationContext, 0.0)
                    mapboxReplayer.play()
                }
            }
        } else {
            MapboxNavigation(optionsBuilder.build())
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }

    private fun shouldRecordHistory(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.nav_native_history_collect_key), false)
    }

    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        originalRoute?.let {
            mapboxNavigation.setRoutes(listOf(it))
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.startTripSession()
        }
    }
}
