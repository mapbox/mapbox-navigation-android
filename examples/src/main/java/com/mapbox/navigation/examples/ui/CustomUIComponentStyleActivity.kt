package com.mapbox.navigation.examples.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
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
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.InstructionViewActivity
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.NavigationButton
import com.mapbox.navigation.ui.SoundButton
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener
import com.mapbox.navigation.ui.feedback.FeedbackItem
import com.mapbox.navigation.ui.instruction.NavigationAlertView
import com.mapbox.navigation.ui.legacy.NavigationConstants
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.OnWayNameChangedListener
import com.mapbox.navigation.ui.summary.SummaryBottomSheet
import com.mapbox.navigation.ui.utils.ViewUtils
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import kotlinx.android.synthetic.main.activity_custom_ui_component_style.*
import okhttp3.Cache
import timber.log.Timber

/**
 * This activity shows how to use your own colors
 * to style the UI SDK components including the InstructionView
 * and SummaryBottomSheet.
 */
class CustomUIComponentStyleActivity : AppCompatActivity(), OnMapReadyCallback,
    FeedbackBottomSheetListener, OnWayNameChangedListener {
    private val replayRouteLocationEngine by lazy { ReplayRouteLocationEngine() }
    private val routeOverviewPadding by lazy { buildRouteOverviewPadding() }

    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private lateinit var speechPlayer: NavigationSpeechPlayer
    private lateinit var destination: LatLng
    private lateinit var summaryBehavior: BottomSheetBehavior<SummaryBottomSheet>
    private lateinit var routeOverviewButton: ImageButton
    private lateinit var cancelBtn: AppCompatImageButton
    private lateinit var feedbackButton: NavigationButton
    private lateinit var instructionSoundButton: NavigationButton
    private lateinit var alertView: NavigationAlertView

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate savedInstanceState=%s", savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ui_component_style)
        initViews()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initNavigation()
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
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()

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
                    LocationComponentActivationOptions.builder(
                        this@CustomUIComponentStyleActivity, style
                    ).build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap, this, true).apply {
                addOnCameraTrackingChangedListener(cameraTrackingChangedListener)
                addProgressChangeListener(mapboxNavigation)
                setCamera(DynamicCamera(mapboxMap))
            }

            LocationEngineProvider.getBestLocationEngine(this)
                .getLastLocation(locationListenerCallback)

            Snackbar.make(
                findViewById(R.id.navigationLayout),
                R.string.msg_long_press_map_to_place_waypoint,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    // InstructionView Feedback Bottom Sheet listener
    override fun onFeedbackSelected(feedbackItem: FeedbackItem?) {
        feedbackItem?.let { feedback ->
            mapboxMap?.snapshot { snapshot ->
                alertView.showFeedbackSubmitted()
                MapboxNavigation.postUserFeedback(
                    feedback.feedbackType,
                    feedback.description,
                    FeedbackEvent.UI,
                    encodeSnapshot(snapshot)
                )
            }
        }
    }

    override fun onFeedbackDismissed() {
        // do nothing
    }

    override fun onWayNameChanged(wayName: String) {
        wayNameView.updateWayNameText(wayName)
        if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            hideWayNameView()
        } else {
            showWayNameView()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun initViews() {
        startNavigation.apply {
            visibility = View.VISIBLE
            isEnabled = false
            setOnClickListener {
                Timber.d("start navigation")
                if (mapboxNavigation.getRoutes().isNotEmpty()) {
                    replayRouteLocationEngine.assign(mapboxNavigation.getRoutes()[0])

                    updateCameraOnNavigationStateChange(true)
                    navigationMapboxMap?.startCamera(mapboxNavigation.getRoutes()[0])

                    mapboxNavigation.startTripSession()
                }
            }
        }

        summaryBottomSheet.visibility = View.GONE
        summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet).apply {
            isHideable = false
            setBottomSheetCallback(bottomSheetCallback)
        }

        routeOverviewButton = findViewById(R.id.routeOverviewBtn)
        routeOverviewButton.setOnClickListener {
            navigationMapboxMap?.showRouteOverview(routeOverviewPadding)
            recenterBtn.show()
        }

        cancelBtn = findViewById(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            mapboxNavigation.stopTripSession()
        }

        recenterBtn.apply {
            hide()
            addOnClickListener {
                recenterBtn.hide()
                summaryBehavior.isHideable = false
                summaryBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                showWayNameView()
                navigationMapboxMap?.resetPadding()
                navigationMapboxMap?.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            }
        }

        wayNameView.apply {
            setVisible(false)
        }

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

        alertView = instructionView.retrieveAlertView().apply {
            hide()
        }
    }

    private fun updateViews(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                startNavigation.visibility = View.GONE

                summaryBottomSheet.visibility = View.VISIBLE
                recenterBtn.hide()

                instructionView.visibility = View.VISIBLE
                feedbackButton.show()
                instructionSoundButton.show()
                showLogoAndAttribution()
            }
            TripSessionState.STOPPED -> {
                startNavigation.visibility = View.VISIBLE
                startNavigation.isEnabled = false

                summaryBottomSheet.visibility = View.GONE
                recenterBtn.hide()
                hideWayNameView()

                instructionView.visibility = View.GONE
                feedbackButton.hide()
                instructionSoundButton.hide()
            }
        }
    }

    private fun showLogoAndAttribution() {
        summaryBottomSheet.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                navigationMapboxMap?.retrieveMap()?.uiSettings?.apply {
                    val bottomMargin = summaryBottomSheet.measuredHeight
                    setLogoMargins(
                        logoMarginLeft,
                        logoMarginTop,
                        logoMarginRight,
                        bottomMargin
                    )
                    setAttributionMargins(
                        attributionMarginLeft,
                        attributionMarginTop,
                        attributionMarginRight,
                        bottomMargin
                    )
                }
                summaryBottomSheet.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun initNavigation() {
        val accessToken = Utils.getMapboxAccessToken(this)
        mapboxNavigation = MapboxNavigation(
            applicationContext,
            MapboxNavigation.defaultNavigationOptions(this, accessToken),
            replayRouteLocationEngine
        )
        mapboxNavigation.apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerBannerInstructionsObserver(bannerInstructionObserver)
            registerVoiceInstructionsObserver(voiceInstructionsObserver)
        }
    }

    private fun initializeSpeechPlayer() {
        val cache =
            Cache(
                File(application.cacheDir, InstructionViewActivity.VOICE_INSTRUCTION_CACHE),
                10 * 1024 * 1024
            )
        val voiceInstructionLoader =
            VoiceInstructionLoader(application, Mapbox.getAccessToken(), cache)
        val speechPlayerProvider =
            SpeechPlayerProvider(application, Locale.US.language, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    }

    private fun showFeedbackBottomSheet() {
        supportFragmentManager.let {
            FeedbackBottomSheet.newInstance(
                this,
                NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION
            )
                .show(it, FeedbackBottomSheet.TAG)
        }
    }

    private fun encodeSnapshot(snapshot: Bitmap): String {
        screenshotView.visibility = View.VISIBLE
        screenshotView.setImageBitmap(snapshot)
        mapView.visibility = View.INVISIBLE
        val encodedSnapshot = ViewUtils.encodeView(ViewUtils.captureView(mapView))
        screenshotView.visibility = View.INVISIBLE
        mapView.visibility = View.VISIBLE
        return encodedSnapshot
    }

    private fun showWayNameView() {
        wayNameView.updateVisibility(!wayNameView.retrieveWayNameText().isNullOrEmpty())
    }

    private fun hideWayNameView() {
        wayNameView.updateVisibility(false)
    }

    private fun buildRouteOverviewPadding(): IntArray {
        val leftRightPadding =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.route_overview_left_right_padding)
                .toInt()
        val paddingBuffer =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.route_overview_buffer_padding)
                .toInt()
        val instructionHeight =
            (resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.instruction_layout_height) + paddingBuffer).toInt()
        val summaryHeight =
            resources.getDimension(com.mapbox.libnavigation.ui.R.dimen.summary_bottomsheet_height)
                .toInt()
        return intArrayOf(leftRightPadding, instructionHeight, leftRightPadding, summaryHeight)
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

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationMapboxMap?.drawRoute(routes[0])
                startNavigation.visibility = View.VISIBLE
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

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    updateViews(TripSessionState.STARTED)

                    navigationMapboxMap?.addOnWayNameChangedListener(this@CustomUIComponentStyleActivity)
                    navigationMapboxMap?.updateWaynameQueryMap(true)
                }
                TripSessionState.STOPPED -> {
                    updateViews(TripSessionState.STOPPED)

                    if (mapboxNavigation.getRoutes().isNotEmpty()) {
                        navigationMapboxMap?.removeRoute()
                    }

                    navigationMapboxMap?.removeOnWayNameChangedListener(this@CustomUIComponentStyleActivity)
                    navigationMapboxMap?.updateWaynameQueryMap(false)

                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            instructionView.updateDistanceWith(routeProgress)
            summaryBottomSheet.update(routeProgress)
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

    private val cameraTrackingChangedListener = object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingChanged(currentMode: Int) {
        }

        override fun onCameraTrackingDismissed() {
            if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
                summaryBehavior.isHideable = true
                summaryBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                hideWayNameView()
            }
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                recenterBtn.show()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }
    }

    private val locationListenerCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: CustomUIComponentStyleActivity) :
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
}
