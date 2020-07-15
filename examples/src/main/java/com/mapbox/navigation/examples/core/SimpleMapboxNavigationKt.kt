package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.camera.CameraPosition
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
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
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
import java.util.Date
import java.util.Locale
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.android.synthetic.main.bottom_sheet_faster_route.*
import kotlinx.android.synthetic.main.content_simple_mapbox_navigation.*
import okhttp3.Cache
import timber.log.Timber

/**
 * This activity shows how to set up a basic turn-by-turn
 * navigation experience with many of the the Navigation SDK's
 * [MapboxNavigation] class' capabilities.
 */
class SimpleMapboxNavigationKt : AppCompatActivity(), OnMapReadyCallback,
    VoiceInstructionsObserver {

    private val VOICE_INSTRUCTION_CACHE =
        "voice-instruction-cache"
    private val startTimeInMillis = 5000L
    private val countdownInterval = 10L
    private val maxProgress = startTimeInMillis / countdownInterval

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var symbolManager: SymbolManager? = null
    private var fasterRoutes: List<DirectionsRoute> = emptyList()

    private lateinit var mapboxNavigation: MapboxNavigation
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
                MapboxNavigation.postUserFeedback(
                    FeedbackEvent.GENERAL_ISSUE,
                    "User feedback test at: ${Date().time}",
                    FeedbackEvent.UI,
                    null
                )
            }
        }
        findViewById<Button>(R.id.btn_clear_routes)?.let { button ->
            button.setOnClickListener {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
        initViews()
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
        mapboxNavigation = getMapboxNavigation(mapboxNavigationOptions)
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
                        .build()
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

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true)
            navigationMapboxMap.setCamera(DynamicCamera(mapboxMap))
            navigationMapboxMap.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap.setOnRouteSelectionChangeListener { route ->
                mapboxNavigation.setRoutes(mapboxNavigation.getRoutes().toMutableList().apply {
                    remove(route)
                    add(0, route)
                })
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

    @SuppressLint("MissingPermission")
    private fun initViews() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFasterRoute)
        bottomSheetBehavior.peekHeight = 0
        fasterRouteAcceptProgress.max = maxProgress.toInt()
        startNavigation.setOnClickListener {
            mapboxNavigation.registerVoiceInstructionsObserver(this)
            navigationMapboxMap.showAlternativeRoutes(false)
        }
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

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            Timber.d("route progress %s", routeProgress.toString())
            navigationMapboxMap.onNewRouteProgress(routeProgress)
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isEmpty()) {
                navigationMapboxMap.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.COMPASS)
                val cameraPosition = CameraPosition.Builder()
                    .tilt(0.0)
                    .padding(doubleArrayOf(0.0, 0.0, 0.0, 0.0))
                    .bearing(0.0)
                    .build()
                mapboxMap?.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                navigationMapboxMap.removeRoute()
                symbolManager?.deleteAll()
            } else {
                navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap.startCamera(routes.first())
                navigationMapboxMap.drawRoutes(routes)
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
        override fun onFasterRoute(currentRoute: DirectionsRoute, alternatives: List<DirectionsRoute>, isAlternativeFaster: Boolean) {
            if (isAlternativeFaster) {
                this@SimpleMapboxNavigationKt.fasterRoutes = alternatives
                fasterRouteSelectionTimer.start()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
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

        mapboxNavigation.startTripSession()
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.attachFasterRouteObserver(fasterRouteObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.detachFasterRouteObserver()
        mapboxNavigation.unregisterVoiceInstructionsObserver(this)
        mapboxNavigation.stopTripSession()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()

        speechPlayer.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
        speechPlayer.play(voiceInstructions)
    }

    private fun getMapboxNavigation(optionsBuilder: NavigationOptions.Builder): MapboxNavigation {
        return if (shouldSimulateRoute()) {
            optionsBuilder.locationEngine(ReplayLocationEngine(mapboxReplayer))
            MapboxNavigation(optionsBuilder.build()).apply {
                registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
                mapboxReplayer.pushRealLocation(applicationContext, 0.0)
                mapboxReplayer.play()
            }
        } else {
            MapboxNavigation(optionsBuilder.build())
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
            .getBoolean(this.getString(R.string.simulate_route_key), false)
    }
}
