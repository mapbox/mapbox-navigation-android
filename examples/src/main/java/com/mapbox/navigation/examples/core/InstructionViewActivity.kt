package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
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
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.NavigationButton
import com.mapbox.navigation.ui.SoundButton
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.legacy.NavigationConstants
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.utils.ViewUtils
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import kotlinx.android.synthetic.main.activity_instruction_view_layout.*
import okhttp3.Cache
import timber.log.Timber

class InstructionViewActivity : AppCompatActivity(), OnMapReadyCallback,
    FeedbackBottomSheetListener {

    private val replayRouteLocationEngine by lazy { ReplayRouteLocationEngine() }

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var locationEngine: LocationEngine
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var speechPlayer: NavigationSpeechPlayer
    private lateinit var destination: LatLng

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var feedbackButton: NavigationButton? = null
    private var instructionSoundButton: NavigationButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate savedInstanceState=%s", savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction_view_layout)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        initNavigation()
        initListeners()
        initializeSpeechPlayer()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()

        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        stopLocationUpdates()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)

        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()

        speechPlayer.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        Timber.d("onMapReady")
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.addOnMapLongClickListener { latLng ->
            Timber.d("onMapLongClickListener position=%s", latLng)
            destination = latLng
            locationComponent?.lastKnownLocation?.let { originLocation ->
                mapboxNavigation.requestRoutes(
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

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@InstructionViewActivity, style)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap).also {
                it.addProgressChangeListener(mapboxNavigation)
                it.setCamera(DynamicCamera(mapboxMap))
            }
        }
    }

    // InstructionView Feedback Bottom Sheet listener
    override fun onFeedbackSelected(feedbackItem: FeedbackItem?) {
        feedbackItem?.let { feedback ->
            mapboxMap?.snapshot { snapshot ->
                MapboxNavigation.postUserFeedback(
                    TelemetryUtils.obtainUniversalUniqueIdentifier(),
                    feedback.feedbackType, feedback.description, encodeSnapshot(snapshot)
                )
            }
        }
    }

    override fun onFeedbackDismissed() {
        // do nothing
    }

    private fun initNavigation() {
        val accessToken = Utils.getMapboxAccessToken(this)
        mapboxNavigation = if (Utils.shouldSimulateRoute(applicationContext)) {
            Timber.d("initNavigation simulate route")
            MapboxNavigation(
                applicationContext,
                accessToken,
                MapboxNavigation.defaultNavigationOptions(this, accessToken),
                replayRouteLocationEngine
            )
        } else {
            Timber.d("initNavigation location engine")
            MapboxNavigation(
                applicationContext,
                accessToken,
                MapboxNavigation.defaultNavigationOptions(this, accessToken)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            Timber.d("start navigation")
            if (mapboxNavigation.getRoutes().isNotEmpty()) {
                if (Utils.shouldSimulateRoute(applicationContext)) {
                    replayRouteLocationEngine.assign(mapboxNavigation.getRoutes()[0])
                }

                navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
                navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])

                mapboxNavigation.startTripSession()
            }
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

    private fun startLocationUpdates() {
        val requestLocationUpdateRequest =
            LocationEngineRequest.Builder(1000L)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .build()

        locationEngine.requestLocationUpdates(
            requestLocationUpdateRequest,
            locationListenerCallback,
            mainLooper
        )
        locationEngine.getLastLocation(locationListenerCallback)
    }

    private fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationListenerCallback)
    }

    private fun showFeedbackBottomSheet() {
        supportFragmentManager?.let {
            FeedbackBottomSheet.newInstance(
                this,
                NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION
            )
                .show(it, FeedbackBottomSheet.TAG)
        }
    }

    private fun encodeSnapshot(snapshot: Bitmap): String {
        screenshotView.visibility = VISIBLE
        screenshotView.setImageBitmap(snapshot)
        mapView.visibility = View.INVISIBLE
        val encodedSnapshot = ViewUtils.encodeView(ViewUtils.captureView(mapView))
        screenshotView.visibility = View.INVISIBLE
        mapView.visibility = VISIBLE
        return encodedSnapshot
    }

    private fun initViews() {
        startNavigation.visibility = VISIBLE
        startNavigation.isEnabled = false
        instructionView.visibility = GONE
        feedbackButton = instructionView.retrieveFeedbackButton().apply {
            hide()
            addOnClickListener {
                showFeedbackBottomSheet()
            }
        }
        instructionSoundButton = instructionView.retrieveSoundButton().apply {
            hide()
            addOnClickListener {
                val soundButton = instructionSoundButton
                if (soundButton is SoundButton) {
                    speechPlayer.isMuted = soundButton.toggleMute()
                }
            }
        }
    }

    private fun updateViews(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                startNavigation.visibility = GONE
                instructionView.visibility = VISIBLE
                feedbackButton?.show()
                instructionSoundButton?.show()
            }
            TripSessionState.STOPPED -> {
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = false
                instructionView.visibility = GONE
                feedbackButton?.hide()
                instructionSoundButton?.hide()
            }
        }
    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap.drawRoute(routes[0])
                startNavigation.visibility = VISIBLE
                startNavigation.isEnabled = true
            } else {
                startNavigation.isEnabled = false
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

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

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    updateViews(TripSessionState.STARTED)
                    stopLocationUpdates()
                    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                    mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionObserver)
                    mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
                }
                TripSessionState.STOPPED -> {
                    updateViews(TripSessionState.STOPPED)
                    startLocationUpdates()
                    mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionObserver)
                    mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                }
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            instructionView.updateDistanceWith(routeProgress)
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

    private class MyLocationEngineCallback(activity: InstructionViewActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            result.locations.firstOrNull()?.let { location ->
                Timber.d("location engine callback -> onSuccess location:%s", location)
                activityRef.get()?.locationComponent?.forceLocationUpdate(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.e("location engine callback -> onFailure(%s)", exception.localizedMessage)
        }
    }

    companion object {
        private const val VOICE_INSTRUCTION_CACHE = "voice-instruction-cache"
    }
}
